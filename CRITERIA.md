# Acceptance criteria — Cold Snow

A box is checked **only with named evidence** (a CI run URL, a command's output, or a file
contents excerpt). Never check from memory. A skipped check never counts as PASS. If CI fails,
the code is fixed and pushed again — no check is weakened and no `|| true` is added to an
assertion path.

## Source and licensing

- [x] `LICENSE` is the full GPL-3.0 text.
  Evidence: `wc -c LICENSE` → 35149 bytes; first line `GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007`.
- [x] `MPL-2.0.txt` is the full MPL-2.0 text; upstream commit SHA is pinned in notices.
  Evidence: `wc -c MPL-2.0.txt` → 16726 bytes; `grep -c ee41b95e4f49c07bea0969062e1260428c499550 THIRD_PARTY_NOTICES.md` → 1.
- [x] Every copied particle source file retains the MPL-2.0 notice header; all 22 PNGs and
      the three particle JSONs are listed in `THIRD_PARTY_NOTICES.md`.
  Evidence: `grep -L "Mozilla Public License" src/main/java/itamio/coldsnow/client/particle/derived/*.java src/main/java/itamio/coldsnow/mixin/LevelRendererMixin.java` printed nothing;
  `find src/main/resources/assets/coldsnow/textures/particle -name '*.png' | wc -l` → 22.
- [x] Shipped `mods.toml` declares `license="GPL-3.0-only"`.
  Evidence: `unzip -p build/libs/coldsnow-1.20.1-forge-1.0.0.jar META-INF/mods.toml | grep ^license=` → `license="GPL-3.0-only"`.

## Build (CI only — no local compilation)

- [x] GitHub repo exists and is public; `build.yml` runs on push/PR/manual.
  Evidence: https://github.com/FrameFlags/ColdSnow — public; `.github/workflows/build.yml` has `on: [push, pull_request, workflow_dispatch]`.
- [x] `build` workflow is green on the release commit.
  Evidence: https://github.com/FrameFlags/ColdSnow/actions/runs/45742689523 — success on `2c4cc074a9b151d156949e05e7e7f647c5628b65`.
- [x] JUnit tests for timeline boundaries, determinism, depth policy and config recovery pass in CI.
  Evidence: CI run above, `> Task :test`, `BUILD SUCCESSFUL in 1m 29s`; downloaded test report `index.html` shows `14` tests, `0` failures across
  `StormTimelineTest`, `SnowDepthPolicyTest`, `ConfigStoreTest`.
- [x] Product jar is non-empty and contains classes plus `META-INF/mods.toml` and particle assets.
  Evidence: CI step `Verify product jar` printed `Product jar: build/libs/coldsnow-1.20.1-forge-1.0.0.jar`;
  `unzip -l` on the downloaded artifact → 29 `.class` entries, `META-INF/mods.toml`,
  `assets/coldsnow/particles/snow.json`, `assets/coldsnow/textures/particle/snow_0.png`.
  SHA-256: `39fa533d0b7f009c35ca333fe6b3be6cfc4597da2ac41812cca5039a89747936`.
- [x] No local `gradlew`/`gradle build` was run for acceptance.
  Evidence: only `javap` API inspection was run locally; all compilation happened in the CI run above.

## Behaviour (manual, owner to verify in-game — not proven by CI)

- [ ] A new snowstorm starts as light flakes, then heavy snow, then blizzard, then tapers.
- [ ] A full cycle lasts between 14 and 20 Minecraft days with default config.
- [ ] Snow accumulates above vanilla's 8-layer cap up to the configured depth (default 16).
- [ ] Entities are pushed up by accumulating snow; no vanilla snow layer pops off above a full block.
- [ ] Vanilla snowflakes are not drawn during a Cold Snow storm; ported flakes are animated and translucent.
- [ ] `/weather clear` does not permanently break an active Cold Snow cycle.
- [ ] Config files survive a restart; malformed config is backed up as `*.invalid`.
- [ ] Client density/accessibility settings do not change server storm timing.

## Publishing

- [x] No release, Modrinth or CurseForge upload was performed without explicit owner authorization.
- [ ] If publishing later: display name avoids exact-name duplicates, original link + license +
      differences are stated (see `ATTRIBUTION_AUDIT.md` in the parent workspace).

## Evidence log

| Check | Evidence (run URL / command output) | Date |
|---|---|---|
| Build + 14 tests | https://github.com/FrameFlags/ColdSnow/actions/runs/45742689523 | 2026-11-26 |
| Artifact | `coldsnow-1.20.1-forge-1.0.0.jar`, SHA-256 `39fa533d…89747936` | 2026-11-26 |
| Earlier failed runs (fixed and replaced) | https://github.com/FrameFlags/ColdSnow/actions/runs/45740160051, /45740542855, /45741991096, /45742356344 | 2026-11-26 |
