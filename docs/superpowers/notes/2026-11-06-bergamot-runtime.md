# Bergamot runtime on desktop: decision record (2026-11-06)

**Decision: option 2. Use the native `translator-cli` built per OS in CI. Download it on demand with a pinned sha256 and run it with `ProcessBuilder` (text on stdin, translation on stdout).**

Option 1 (WASM in the JVM under Chicory) does not work with Mozilla's builds. The reasons are below.

## Models (Step 1)

- Index: `https://firefox.settings.services.mozilla.com/v1/buckets/main/collections/translations-models/records` (742 records).
- Attachment base: `https://firefox-settings-attachments.cdn.mozilla.net/`, then `attachment.location`.
- Each record is one file. It has `fromLang`, `toLang`, `fileType` (`model`, `lex`, `vocab`, or `srcvocab` + `trgvocab`), `version`, and `attachment.{filename,size,hash(sha256),location,mimetype}`. A pair can appear in several versions, so pick the highest `version`.
- Attachments are **not compressed**. The CDN sends `content-encoding: identity`. Store the files as they arrive and check them against `attachment.hash`.
- ja→en v2.1 (the latest) has 3 files, 54.8 MB in total:

| file | bytes | gzip -6 (for reference) |
|---|---|---|
| `model.jaen.intgemm.alphas.bin` | 43,977,787 | 32.6 MB |
| `lex.50.50.jaen.s2t.bin` | 9,348,172 | 4.8 MB |
| `vocab.jaen.spm` (shared src/trg vocab) | 1,443,222 | 0.7 MB |

- en→ja v2.3 uses separate `srcvocab.enja.spm` and `trgvocab.enja.spm` files. This is why `BergamotModelFiles` has `srcVocab` and `trgVocab`. For a shared vocab, pass the same file twice.

## Option 1: Bergamot WASM under Chicory (rejected)

Two builds were tried:
- Firefox's current engine: Remote Settings `translations-wasm`, `bergamot-translator` 3.0, 4.96 MB.
- The last GitHub release: `mozilla/bergamot-translator` v0.4.4, `bergamot-translator-worker-without-wormhole.wasm`, 5.18 MB.

Results:

- **SIMD.** Both builds contain wasm SIMD128. Chicory 1.7.5's AOT compiler (`MachineFactoryCompiler`) rejects both, with `InterpreterFallback.FAIL` and with `WARN`, with this error: `ChicoryException: Unhandled opcode: V128_CONST`. Chicory supports SIMD only in its interpreter (`chicory-simd`, which uses the incubator Vector API). Running a 44 MB int8 model in an interpreter cannot meet the < 2 s gate.
- **Emscripten/embind glue.** The API (`BlockingService`, `TranslationModel`, `AlignedMemory`, …) exists only through `_embind_register_*` and `_emval_*` imports. To use it, we would have to rewrite emscripten's embind JS runtime in Kotlin. About 40 `env.__syscall_*` and `_mmap_js` imports, plus WASI imports, also need host functions.
- **wasm_gemm imports.** The int8 GEMM comes in as 7 `wasm_gemm.int8_*` imports. Firefox implements them natively. The wasm exports `*Fallback` versions, but they are SIMD too.
- Threads were not a blocker: the memory is unshared and there are no atomics imports.
- The only way left for option 1 is a custom non-SIMD, non-embind (C ABI) wasm build. That means a second toolchain (emscripten or wasi-sdk) and a scalar GEMM running in JVM bytecode. It would cost more than option 2 and run slower.

## Option 2: native `translator-cli` (chosen)

Source: `mozilla/translations` @ `8eada038fd45bbf6fa58a412b22f80c79887fabc`, `inference/` (BERGAMOT_VERSION v0.6.0). The old `mozilla/bergamot-translator` repo has been moved here. The CLI is `inference/src/app/translator_cli.cpp`. It reads all of stdin as one request and prints only the translated text.

### Local build on this Mac (macOS 26, Apple clang 21, arm64)

- Configure: about 3 min. Most of that time is Bergamot's CMake running `git submodule update`, which also pulls submodules it does not need (fbgemm, marian-dev, kenlm, emsdk).
- Clean build of `translator-cli` with `-j16`: **34 s**.
- Blockers found, all fixed:
  1. zlib `zutil.h` defines `fdopen(fd,mode) NULL` when `TARGET_OS_MAC` is set, which breaks the SDK's `_stdio.h`. Fix: also require `!defined(__APPLE__)`.
  2. sentencepiece `trainer_interface.cc`: `constexpr ScriptType kAnyType = static_cast<ScriptType>(-1)` is a hard error in clang 21. Fix: `const`.
  3. marian builds with `-Werror`, and clang 21 adds `-Wvla-cxx-extension`. Fix: `-Wno-error`.
  4. The arm64 defines (`ARM FMA SSE`, `-flax-vector-conversions`) are only set inside `marian-fork/`, so `src/translator` fails to compile with `_mm_*` errors. Fix: pass them through `CMAKE_CXX_FLAGS` at the root.
- The native build of `inference/` is only maintained by Mozilla for x86-64 Linux (`scripts/build.py`: fbgemm + MKL). Every other OS is our own work.
- Flags used: `-DCMAKE_BUILD_TYPE=Release -DCOMPILE_CPU=on -DCOMPILE_CUDA=off -DUSE_FBGEMM=off -DBUILD_ARCH=armv8-a -DSSPLIT_USE_INTERNAL_PCRE2=ON -DCMAKE_OSX_DEPLOYMENT_TARGET=12.0 -DCMAKE_CXX_FLAGS="-DARM -DFMA -DSSE -flax-vector-conversions -fno-strict-aliasing"`. GEMM goes through ruy for int8 and Apple Accelerate for sgemm.

### Results (macOS arm64)

- Binary: 10.1 MB, **7.7 MB stripped**, 3.2 MB gzip -9. It links only system libraries (Accelerate, libiconv, libc++, libSystem). Minimum OS: macOS 12.0.
- `彼は静かに剣を抜いた。` gives **"He pulled out his sword quietly."**
- Latency: one new process per call, including loading the model:
  - first run: 0.38 s
  - warm page cache: **0.12 s**
  - 2 paragraphs / 3 sentences: 0.18 s
- Smoke test from Kotlin (`BergamotCliRuntime` → `ProcessBuilder`): passed, 360 ms on the first call inside the JVM test.

### Install size per OS

| platform | translator download | status |
|---|---|---|
| macos-arm64 | 7.7 MB (7,714,944 bytes) | `bergamot-cli-0.1.1`; installed through `BergamotCliRuntime.install()` and translated ja→en on this Mac |
| macos-x64 | 8.6 MB (8,646,656 bytes) | `bergamot-cli-0.1.1`; not run (no Intel Mac at hand) |
| linux-x64 | 38.1 MB, stripped (static OpenBLAS with all its CPU kernels; dynamic deps: glibc ≥ 2.35, libstdc++, libgcc_s, libm only) | `bergamot-cli-0.1.1`; not run (no Linux machine at hand) |
| windows-x64 | none | no build: 0.1.1 failed at configure (see below); the app reports "Bergamot isn't available for Windows yet. Use Ollama or Custom instead." |

Models are separate downloads: about 55 MB per direction for ja↔en, uncompressed.

### CI

`.github/workflows/bergamot-cli.yml`.

Triggers:
- Pushing a `bergamot-cli-*` tag. This works before the file reaches the default branch, which `workflow_dispatch` requires. The release is created for the existing tag with `--verify-tag`.
- `workflow_dispatch` with a `tag` input. A new tag is created at the commit the run built from.

Steps:
- Checks out the pinned `mozilla/translations` commit and only the submodules the CLI needs.
- Applies the 3 patches with `sed`.
- Builds `translator-cli` for each of the 4 platforms. `USE_STATIC_LIBS` is set only on Linux; on macOS it makes CMake look for `.a` files the SDK doesn't ship, so the macOS legs match the local build.
- Strips the binaries and re-signs the macOS ones ad hoc (`codesign -s - -f`). Stripping removes the signature, and macOS kills unsigned arm64 binaries.
- The release job runs even when some legs fail (`if: !cancelled()`). It publishes only the legs that uploaded an artifact, as `translator-cli-<key>[.exe]`, plus `SHA256SUMS`.
- The tag must match `bergamot-cli-<version>`. The release is created with `--prerelease --latest=false`, so the app's `/releases/latest` links keep pointing at app releases. `selectStable` (`v*` tags only) and `selectNightly` (`nightly_*` only) already ignore it. `PrereleaseRepository.getReleases()` drops `bergamot-cli-*` tags (`GitHubPrerelease.isToolRelease`). Its only caller is `PrereleaseViewModel`.
- Permissions: `contents: read` for the workflow, and `contents: write` only for the release job.

**CI run 1, `bergamot-cli-0.1.0`:**
- linux-x64 succeeded and was published as a prerelease.
- The macOS and Windows legs failed at configure. Their runners have CMake 4.4, which removed compatibility with `cmake_minimum_required` < 3.5, and three projects declare an older minimum: ruy's clog, sentencepiece, and ssplit's ExternalProject PCRE2. The Linux runner had a CMake 3.x.

**Fix:** the workflow-level env var `CMAKE_POLICY_VERSION_MINIMUM=3.5`. It has to be an env var because `-D` does not reach the PCRE2 ExternalProject; a local CMake 4.4 build failed there with `-D` alone. With the env var, the macos-arm64 leg's configuration built and translated correctly locally under CMake 4.4.

**Linux size:** 38 MB is mostly static OpenBLAS. Switching Linux to `USE_RUY_SGEMM` like Windows would probably bring it near 10 MB. It is untested, so it was not changed while the leg works.

**CI run 2, `bergamot-cli-0.1.1`:**
- macos-arm64, macos-x64 and linux-x64 succeeded. Their URLs, sha256 values and sizes are pinned in `pinnedBergamotCliAssets`.
- windows-x64 failed at configure: `configure_file` at `marian-fork/CMakeLists.txt:758` reported `Doxyfile.in does not exist`. The Windows runner has Doxygen installed, so marian's default `USE_DOXYGEN=ON` tried to generate docs, and mozilla/translations doesn't ship marian's Doxyfile.in. Fix: `-DUSE_DOXYGEN=OFF` on all legs.
- That error came at the very end of configure, so ruy, cpuinfo and PCRE2 configured under MSVC. Compiling and linking on Windows is still unproven.

**Platforms without a pinned build:** `BergamotRuntime.isSupported` is false, and `unavailableReason()` returns "Bergamot isn't available for <OS> yet. Use Ollama or Custom instead." Using a local binary via `BERGAMOT_CLI` still works.

**Install check (2026-11-06, this Mac):** the env-gated `BergamotCliRuntimeSmokeTest` "installs the pinned build and translates" (`BERGAMOT_INSTALL_SMOKE=1`) passed:
- download plus sha256 check took 1.7 s (7,714,944 bytes)
- the exec bit was set
- the release binary returned "He pulled out his sword quietly." in 319 ms
- the temp install root was deleted afterwards

## Implementation

`kmpuiviews/src/jvmMain/.../translation/bergamot/BergamotRuntime.kt`:
- `BergamotRuntime`, `BergamotModelFiles`.
- `BergamotCliRuntime`. It finds the CLI in this order: env `BERGAMOT_CLI`, then `<installRoot>/translator-cli`, then the pinned download.
- `bergamotConfigYaml`, which uses Firefox's Marian options with `gemm-precision: int8shiftAlphaAll`. It writes one config per model under `<installRoot>/configs/`.
- `bergamotPlatformKey`.

Smoke test: `kmpuiviews/src/jvmTest/.../translation/bergamot/BergamotCliRuntimeSmokeTest.kt`. The project's JVM tests use JUnit 4, so the env gate is `Assume.assumeTrue(BERGAMOT_SMOKE == "1")` instead of JUnit 5's `@EnabledIfEnvironmentVariable`.

## Open points

- The model is reloaded on every call (about 0.1 s warm, cold disk slower). Long-running modes would need a small C++ driver of our own around `BlockingService`. For descriptions this is fine.
- **Glued sentences, fixed in the runtime.** With 2 sentences in one paragraph, the CLI returned "…quietly.Then she laughed."
  - Cause: Bergamot joins the target sentences with the whitespace that followed each source sentence, and Japanese puts none after `。`. No CLI option changes this.
  - Fix: `spaceBetweenSentences()` runs on every result. It inserts a space where `.`, `!` or `?` comes after a lowercase letter or digit, plus an optional closing curly quote or bracket, and is directly followed by one of these:
    - a capitalized word (not all-caps)
    - an opening curly quote
    - `(`
  - Left alone:
    - decimals ("3.14")
    - abbreviations ("e.g. this", "U.S.A.")
    - all-caps words after a dot ("Node.JS", "example.COM")
    - anything next to a straight `"`. It can't be told apart as opening or closing, so `quietly."Then` stays glued.
  - These cases are unit-tested in `BergamotRuntimeTest`.
- **Cancellation.** `translate` waits for the process inside `runInterruptible`. On timeout, cancellation or any failure, it kills the process before the pipe jobs are awaited.
- Licenses were not checked in this spike. bergamot-translator/marian are MPL-2.0 and MIT. Check the model license before shipping, and add attribution to the app's licenses screen.
