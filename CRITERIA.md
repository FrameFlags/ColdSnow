# Acceptance criteria — Cold Snow

A box is checked **only with named evidence** (a CI run URL, a command's output, or a file
contents excerpt). Never check from memory. A skipped check never counts as PASS. If CI fails,
fix the code and push again — never weaken a check and never add `|| true` to an assertion path.

## Source and licensing

- [ ] `LICENSE` is the full GPL-3.0 text.
  Evidence: `wc -c LICENSE` and first two lines.
- [ ] `MPL-2.0.txt` is the full MPL-2.0 text; upstream commit SHA is pinned in notices.
  Evidence: `wc -c MPL-2.0.txt`; `THIRD_PARTY_NOTICES.md` contains `ee41b95e4f49c07bea0969062e1260428c499550`.
- [ ] Every copied particle source file retains the MPL-2.0 notice header; all 22 PNGs and
      the three particle JSONs are listed in `THIRD_PARTY_NOTICES.md`.
  Evidence: `grep -L "Mozilla Public License" src/main/java/itamio/coldsnow/client/particle/derived/*.java`
      prints nothing; `find src/main/resources/assets/coldsnow/textures/particle -name '*.png' | wc -l` = 22.
- [ ] `mods.toml` declares `license="GPL-3.0-only"`.
  Evidence: file contents.

## Build (CI only — no local compilation)

- [ ] GitHub repo exists and is public; `build.yml` runs on push/PR/manual.
  Evidence: repo URL + workflow file.
- [ ] `build` workflow is green on the release commit.
  Evidence: `gh run view <id>` URL.
- [ ] JUnit tests for timeline boundaries, determinism, depth policy and config recovery pass in CI.
  Evidence: test report artifact / `gh run view --log` excerpt.
- [ ] Product jar is non-empty and contains classes plus `META-INF/mods.toml`.
  Evidence: CI step output containing `Product jar: ...`.
- [ ] No local `gradlew`/`gradle build` was run for acceptance.
  Evidence: this session's tool log.

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

- [ ] No release, Modrinth or CurseForge upload has been performed without explicit owner authorization.
- [ ] If publishing later: display name avoids exact-name duplicates, original link + license +
      differences are stated (see `ATTRIBUTION_AUDIT.md` in the parent workspace).

## Evidence log

| Check | Evidence (run URL / command output) | Date |
|---|---|---|
| (pending) | | |
