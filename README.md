# Cold Snow

A Minecraft **1.20.1 Forge** mod that makes snow feel colder and more alive.

Cold Snow replaces the short, flat vanilla snowfall with a **multi-day storm system**:
snow starts as light flakes, builds into heavy snow, peaks as a full blizzard, then tapers
off. A single storm can last **14–20 Minecraft days** (about 4.7–6.7 real hours of playtime)
and is fully configurable. During a storm, snow piles up far deeper than vanilla allows —
up to **64 layers** by default 16 — using realistic animated particle effects ported from
**Biome Particle Weather / Impactful Weather** by shadowmaster435.

> **Build policy:** this project is never compiled on a workstation. Every build and test runs
> in GitHub Actions; the shipped jar is the CI artifact. See `CRITERIA.md` for evidence.

## Features

- **Storm lifecycle:** `CLEAR → LIGHT → HEAVY → BLIZZARD → TAPER → CLEAR`, driven by a
  deterministic, seeded timeline on the logical server.
- **Long storms:** 14–20 Minecraft days per cycle (33,600–48,000 seconds of game time).
  No client can shorten a storm by changing its own clock.
- **Deep snow:** vanilla caps a snow column at 8 layers. Cold Snow disables vanilla's
  accumulation gamerule and re-implements it, stacking vanilla snow layers and full snow
  blocks up to a configurable 8–64 layers (default 16), with entity push-up and biome checks.
- **Realistic particles:** seven-frame animated snow flakes, seven-frame blizzard flakes,
  and eight-frame wind streaks, with light-dependent colour, fade-in/out and ground shrink.
  Flake fall speed is driven by the synced storm intensity, so snow visibly falls faster as
  the storm builds.
- **Weather sync:** the server owns the storm; clients receive stage/intensity packets for
  rendering. Vanilla snowflake rendering is suppressed while a Cold Snow storm is active.
- **Configurable:** `config/coldsnow.json` (server) and `config/coldsnow-client.json`
  (client). Both are versioned, clamped and atomically written; malformed files are backed
  up as `*.invalid` rather than silently discarded.
- **Accessibility:** client-only density multiplier and toggles for wind trails and screen
  effects.

## Configuration

`config/coldsnow.json` (server — affects everyone):

| Key | Default | Range | Meaning |
|---|---:|---:|---|
| `enabled` | `true` | boolean | Master switch |
| `cycleDaysMin` / `cycleDaysMax` | `14` / `20` | 1–365 | Storm duration bounds in Minecraft days |
| `seed` | `42` | long | Deterministic cycle-length seed |
| `maxLayers` | `16` | 8–64 | Deepest snow column |
| `accumulationIntervalTicks` | `20` | ≥1 | Throttle for the accumulation scan |

`config/coldsnow-client.json` (client — local only):

| Key | Default | Range | Meaning |
|---|---:|---:|---|
| `particleDensity` | `1.0` | 0–2 | Flake multiplier |
| `respectVanillaParticleSetting` | `true` | boolean | Honour vanilla particle settings |
| `disableWindTrails` | `false` | boolean | Hide blizzard wind streaks |
| `disableScreenFlash` | `true` | boolean | Disable rapid screen effects |

## Installation

1. Install **Forge 47.4.20+** for Minecraft **1.20.1**.
2. Put `coldsnow-1.20.1-forge-1.0.0.jar` in your `mods/` folder.
3. Start the world. Snow weather begins a Cold Snow cycle automatically.
4. Optional: `/gamerule doWeatherCycle true` is overridden while the mod is active; the mod
   controls weather on the overworld itself.

## Attribution and license

Cold Snow is **GPL-3.0-only**. The animated snow/blizzard/wind particle source files and 22
particle textures are derived from **Impactful Weather / Biome Particle Weather** by
**shadowmaster435**, upstream https://github.com/shadowmaster435/Impactful_Weather at commit
`ee41b95e4f49c07bea0969062e1260428c499550`. Those files remain **MPL-2.0**, with notices
retained. Upstream declares MPL-2.0 in its LICENSE but GPLv3 in its mod metadata and CurseForge
page; Cold Snow takes the stricter declaration for the combined work. Full details in
`THIRD_PARTY_NOTICES.md`; full license texts in `LICENSE` and `MPL-2.0.txt`.

No vanilla Minecraft assets are redistributed.

## Verification status

See `CRITERIA.md` for the evidence table. CI builds and unit tests are the only automated
checks; **in-game behaviour is not verified by a green build** and requires a manual run by
the owner.
