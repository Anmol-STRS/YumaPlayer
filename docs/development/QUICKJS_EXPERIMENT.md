# QuickJS Migration Experiment (Stages 1 & 2)

## Status: Inert Module Port Complete

- **Module**: `:morideobfuscator-quickjs`
- **Package**: `moe.rukamori.archivetune.obfuscator.quickjs.youtubei`
- **Dependency**: `io.github.dokar3:quickjs-kt:1.0.15`
- **Consumer**: None (standalone inert port, zero changes to active playback or `:app`)

## Asset Integrity (Byte-Identical Upstream ArchiveTune)

Verified SHA-256:
- `runtime-polyfills.js`: `3237147e79e82079fa0081abb834a01310c3347af6682686c42d406850531248`
- `youtubei.bundle.js`: `dcbee3ed43ead7aaf2871e416e0201801c8e5befac74e27c89e5c8a1e923e16b`

Assets location: `morideobfuscator-quickjs/src/main/assets/youtubei-quickjs/`

## Isolation & Boundaries

- Cache path isolated: `noBackupFilesDir/youtubei-quickjs/18.0.0` (distinct from Rhino/other cache).
- JS build provenance retained under `morideobfuscator-quickjs/javascript/` without `node_modules` or build artifacts.
- No changes to existing `:morideobfuscator`, `:core`, or `:app`.

## Runtime & Lifecycle Observations

- Upstream `YoutubeiQuickJsWorker.closeRuntime()` releases the `QuickJs` native instance (`runtime.close()`), but does not shut down the single-thread `dispatcher` executor (`Executors.newSingleThreadExecutor`). This matches upstream behavior and avoids recreation overhead, but the thread remains alive as a daemon.
- Pure ST / signature / n-transform API requires future JS export adaptation; not implemented in this stage.

## Stage 3 Adaptation: Standalone Cipher Facade & Export

- **Status**: Completed.
- **JS Bundle Rebuild**:
  - Command: `npm run bundle` (`node scripts/bundle.mjs` using pinned `esbuild@0.28.2`).
  - `runtime-polyfills.js` SHA-256: `3237147e79e82079fa0081abb834a01310c3347af6682686c42d406850531248` (unchanged).
  - `youtubei.bundle.js` SHA-256: `0cbbf267f6531a8b7b6d3aa733a4570438f1060d3425b4c9ce8956052d77dd87` (diverged: exported narrow `ArchiveTuneYoutubei.decipherUrl` and safe platform guards).
- **Public API**:
  - `QuickJsPlayerScript` (immutable script descriptor: `playerId`, `source`, `url`).
  - `QuickJsPlayerScriptProvider` (`suspend fun getPlayerScript(videoId: String): QuickJsPlayerScript`).
  - `QuickJsCipher` facade exposing:
    - `signatureTimestamp(videoId: String): Result<Int?>`: pure Kotlin regex extraction (`signatureTimestamp\s*:\s*(\d{5,})`), never initializes QuickJS or AST analyzer.
    - `transformN(videoId: String, url: String): Result<String>`: direct URLs without `n` query parameter return immediately without provider or runtime execution; URLs with `n` invoke `decipherUrl`.
    - `resolveStreamUrl(videoId: String, cipher: String): Result<String>`: form query decoded once; missing/blank `sp` defaults to `"signature"`; missing `url` fails with typed `IllegalArgumentException`; direct HTTP(S) URL without `s` or `n` returns immediately.
- **Worker & Lifecycle**:
  - `QuickJsCipherWorker`: dedicated single-thread daemon executor, loads polyfills and bundle, connects disk cache (`youtubei-quickjs/18.0.0`) and host crypto/UUID bindings.
  - Suspending `close()`: serialized via `mutex.withLock` on the worker dispatcher; shuts down executor; subsequent operations fail explicitly with `IllegalStateException`.
  - Caching by composite key `${script.playerId}_${sha256(script.source)}_v1`: passes `playerSource` to JS only on first compilation/change; subsequent calls omit source and reuse player in memory/disk cache without AST reparsing.
  - Cancellation: `CancellationException` is explicitly rethrown, never captured in `Result.failure`.
- **Evidence & Verification**:
  - Offline Node suite (`test/decipher-test.mjs`): 8/8 tests passed against rebuilt bundle (signatureCipher default sp, custom sp, combined sig+n, direct URL with n-only, disk cache persistence across memory reset, missing capability failure, malformed input failure, player hash update/isolation).
  - Kotlin unit tests (`:morideobfuscator-quickjs:testDebugUnitTest`): 9/9 tests passed (ST regex, malformed ST, no-n bypass, no-cipher bypass, missing URL failure, non-URL input failure, post-close failures, cancellation propagation, query decoding).
  - Native runtime note: Node tests verify actual bundle JS logic and exports; native QuickJS Android execution remains bounded to device/emulator runtime.
