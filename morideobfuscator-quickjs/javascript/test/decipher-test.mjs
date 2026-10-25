import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const bundlePath = resolve(__dirname, "../../src/main/assets/youtubei-quickjs/youtubei.bundle.js");
const polyfillsPath = resolve(__dirname, "../../src/main/assets/youtubei-quickjs/runtime-polyfills.js");

const polyfills = await readFile(polyfillsPath, "utf8");
const bundle = await readFile(bundlePath, "utf8");

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

new Function(polyfills)();
new Function(bundle)();

const runtime = globalThis.ArchiveTuneYoutubei;
assert.ok(runtime);
assert.equal(typeof runtime.decipherUrl, "function");

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
      if (n === "error_trigger") {
        this.map.set("n", "enhanced_except_bad_token");
      } else {
        this.map.set("n", "deciphered_" + n);
      }
    }
  };

  var nsigFunc = function(u, sp = "signature", s = "val") {
    u = new ns.Url(u);
    u.set("alr", "yes");
    u.set("s", s);
    return u;
  };
})(this);`;

{
  const request = JSON.stringify({
    playerId: "player1_hashA_v1",
    playerSource: syntheticPlayerSource,
    signatureTimestamp: 19842,
    cipher: "s=testSig123&url=https%3A%2F%2Fgooglevideo.com%2Fvideoplayback%3Fexpire%3D1000",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("signature=deciphered_testSig123"));
}

{
  const request = JSON.stringify({
    playerId: "player1_hashA_v1",
    cipher: "s=customSig456&sp=sig&url=https%3A%2F%2Fgooglevideo.com%2Fvideoplayback%3Fexpire%3D1000",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("sig=deciphered_customSig456"));
}

{
  const request = JSON.stringify({
    playerId: "player1_hashA_v1",
    cipher: "s=combSig&sp=signature&url=https%3A%2F%2Fgooglevideo.com%2Fvideoplayback%3Fexpire%3D1000%26n%3DoriginalN",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("signature=deciphered_combSig"));
  assert.ok(res.value.includes("n=deciphered_originalN"));
}

{
  const request = JSON.stringify({
    playerId: "player1_hashA_v1",
    url: "https://googlevideo.com/videoplayback?expire=1000&n=directNVal",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("n=deciphered_directNVal"));
}

{
  runtime.reset();
  const request = JSON.stringify({
    playerId: "player1_hashA_v1",
    url: "https://googlevideo.com/videoplayback?expire=1000&n=cachedN",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("n=deciphered_cachedN"));
}

{
  const request = JSON.stringify({
    playerId: "unknownPlayer",
    url: "https://googlevideo.com/videoplayback?expire=1000&n=val",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, false);
  assert.equal(res.error.kind, "DECIPHER");
}

{
  const request = JSON.stringify({
    url: "https://googlevideo.com/videoplayback?expire=1000",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, false);
  assert.equal(res.error.kind, "INTERNAL");
}

{
  const newSource = syntheticPlayerSource.replaceAll("deciphered_", "v2_deciphered_");
  const request = JSON.stringify({
    playerId: "player1_hashB_v1",
    playerSource: newSource,
    signatureTimestamp: 19842,
    url: "https://googlevideo.com/videoplayback?expire=1000&n=tokenX",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  assert.ok(res.value.includes("n=v2_deciphered_tokenX"));

  const oldRequest = JSON.stringify({
    playerId: "player1_hashA_v1",
    url: "https://googlevideo.com/videoplayback?expire=1000&n=tokenX",
  });
  const oldRes = JSON.parse(await runtime.decipherUrl(oldRequest));
  assert.equal(oldRes.ok, true);
  assert.ok(oldRes.value.includes("n=deciphered_tokenX"));
}

{
  const request = JSON.stringify({
    playerId: "player1_hashA_v1",
    url: "https://googlevideo.com/videoplayback?c=WEB&cver=custom.123&pot=my_pot_token&expire=1000&n=tokenX",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  const parsed = new URL(res.value);
  assert.equal(parsed.searchParams.get("c"), "WEB");
  assert.equal(parsed.searchParams.get("cver"), "custom.123");
  assert.equal(parsed.searchParams.get("pot"), "my_pot_token");
}

{
  const request = JSON.stringify({
    playerId: "player1_hashA_v1",
    url: "https://googlevideo.com/videoplayback?expire=1000&n=error_trigger",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, false);
  assert.equal(res.error.kind, "DECIPHER");
  assert.ok(res.error.message.includes("enhanced_except_bad_token"));
}

{
  const request = JSON.stringify({
    playerId: "player1_hashA_v1",
    url: "https://googlevideo.com/videoplayback?expire=1000&query=a%2Bb&nested=x%2520y&n=directVal",
  });
  const res = JSON.parse(await runtime.decipherUrl(request));
  assert.equal(res.ok, true);
  const parsed = new URL(res.value);
  assert.equal(parsed.searchParams.get("query"), "a+b");
  assert.equal(parsed.searchParams.get("nested"), "x%20y");
  assert.equal(parsed.searchParams.get("n"), "deciphered_directVal");
}
