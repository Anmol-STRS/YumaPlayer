import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const bundlePath = resolve(__dirname, "../../src/main/assets/youtubei-quickjs/youtubei.bundle.js");
const polyfillsPath = resolve(__dirname, "../../src/main/assets/youtubei-quickjs/runtime-polyfills.js");

// Load polyfills and bundle in Node environment
const polyfills = await readFile(polyfillsPath, "utf8");
const bundle = await readFile(bundlePath, "utf8");

// Set up mock host bindings
const diskCache = new Map();
globalThis.__archiveTuneCacheRead = async (key) => diskCache.get(key) || null;
globalThis.__archiveTuneCacheWrite = async (req) => {
  const { key, value } = JSON.parse(req);
  diskCache.set(key, value);
};
globalThis.__archiveTuneCacheRemove = async (key) => {
  diskCache.delete(key);
};
globalThis.__archiveTuneSha1 = async (val) => {
  const buf = await crypto.subtle.digest("SHA-1", new TextEncoder().encode(val));
  return Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, "0")).join("");
};
globalThis.__archiveTuneUuid = () => crypto.randomUUID();
globalThis.__archiveTuneRandom = (size) => {
  const bytes = new Uint8Array(size);
  crypto.getRandomValues(bytes);
  return Buffer.from(bytes).toString("base64");
};

// Evaluate polyfills and bundle
new Function(polyfills)();
new Function(bundle)();

const runtime = globalThis.ArchiveTuneYoutubei;
assert.ok(runtime, "ArchiveTuneYoutubei must be defined on globalThis");
assert.equal(typeof runtime.decipherUrl, "function", "decipherUrl must be exported");

// Synthetic player script fixture matching modern YouTube base.js structure
const syntheticPlayerSource = `(function(g) {
  var ns = {};
  ns.Url = class {
    constructor(u) {
      this.map = new Map();
      try {
        const parsed = new URL(u);
        parsed.searchParams.forEach((v, k) => this.map.set(k, v));
      } catch (_) {
        this.map.set("url", u);
      }
    }
    set(k, v) { this.map.set(k, v); }
    get(k) { return this.map.get(k); }
    transform() {
      var s = this.map.get("s") || "default_sig";
      this.map.set("sig", "deciphered_" + s);
      this.map.set("signature", "deciphered_" + s);
      var n = this.map.get("n") || "default_n";
      this.map.set("n", "deciphered_" + n);
    }
  };

  var nsigFunc = function(u, sp = "signature", s = "val") {
    u = new ns.Url(u);
    u.set("alr", "yes");
    u.set("s", s);
    return u;
  };
})(this);`;

console.log("[TEST] 1. signatureCipher with default sp");
{
  const request = JSON.stringify({
    playerId: "player1",
    playerSource: syntheticPlayerSource,
    signatureTimestamp: 19842,
    cipher: "s=testSig123&sp=signature&url=https%3A%2F%2Fgooglevideo.com%2Fvideoplayback%3Fexpire%3D1000",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("signature=deciphered_testSig123"), `Expected signature in ${res.value}`);
  assert.ok(res.value.includes("https://googlevideo.com/videoplayback"), `Expected base URL in ${res.value}`);
}

console.log("[TEST] 2. signatureCipher with custom sp");
{
  const request = JSON.stringify({
    playerId: "player1",
    cipher: "s=customSig456&sp=sig&url=https%3A%2F%2Fgooglevideo.com%2Fvideoplayback%3Fexpire%3D1000",
  });
  // Note: player1 is cached, so playerSource is omitted
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("sig=deciphered_customSig456"), `Expected sig in ${res.value}`);
}

console.log("[TEST] 3. Combined signature + n-token transform");
{
  const request = JSON.stringify({
    playerId: "player1",
    cipher: "s=combSig&sp=signature&url=https%3A%2F%2Fgooglevideo.com%2Fvideoplayback%3Fexpire%3D1000%26n%3DoriginalN",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("signature=deciphered_combSig"), `Expected signature in ${res.value}`);
  assert.ok(res.value.includes("n=deciphered_originalN"), `Expected transformed n in ${res.value}`);
}

console.log("[TEST] 4. Direct URL with n-token only");
{
  const request = JSON.stringify({
    playerId: "player1",
    url: "https://googlevideo.com/videoplayback?expire=1000&n=directNVal",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("n=deciphered_directNVal"), `Expected transformed n in ${res.value}`);
}

console.log("[TEST] 5. Cache reuse from disk across reset");
{
  runtime.reset(); // clears in-memory activePlayers
  // Call without playerSource - should load from diskCache
  const request = JSON.stringify({
    playerId: "player1",
    url: "https://googlevideo.com/videoplayback?expire=1000&n=cachedN",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true, `Expected success from disk cache, got: ${JSON.stringify(res)}`);
  assert.ok(res.value.includes("n=deciphered_cachedN"));
}

console.log("[TEST] 6. Missing player capability (uncached player without source)");
{
  const request = JSON.stringify({
    playerId: "unknownPlayer",
    url: "https://googlevideo.com/videoplayback?expire=1000&n=val",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, false);
  assert.equal(res.error.kind, "DECIPHER");
}

console.log("[TEST] 7. Malformed input (missing playerId)");
{
  const request = JSON.stringify({
    url: "https://googlevideo.com/videoplayback?expire=1000",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, false);
  assert.equal(res.error.kind, "INTERNAL");
}

console.log("[TEST] 8. Player hash update / new player isolation");
{
  const newPlayerSource = syntheticPlayerSource.replaceAll("deciphered_", "v2_deciphered_");
  const request = JSON.stringify({
    playerId: "player2",
    playerSource: newPlayerSource,
    signatureTimestamp: 19843,
    url: "https://googlevideo.com/videoplayback?expire=1000&n=token2",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("n=v2_deciphered_token2"), `Expected v2 transform in ${res.value}`);

  // Ensure player1 is unchanged
  const req1 = JSON.stringify({
    playerId: "player1",
    url: "https://googlevideo.com/videoplayback?expire=1000&n=token1",
  });
  const res1 = JSON.parse(await runtime.decipherUrl(req1));
  assert.equal(res1.ok, true);
  assert.ok(res1.value.includes("n=deciphered_token1"), `Expected player1 transform in ${res1.value}`);
}

console.log("All Node tests PASSED successfully!");
