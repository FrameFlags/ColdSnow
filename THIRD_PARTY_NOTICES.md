# Third-party notices — Cold Snow

Cold Snow is licensed **GPL-3.0-only** (see `LICENSE`). Some files below are derived from
another project and remain under their original license, as permitted by MPL-2.0 §3.3
("Larger Work"). Nothing here claims original authorship of upstream work.

## Upstream project

- **Project:** Impactful Weather / "Biome Particle Weather"
- **Author:** shadowmaster435 (CurseForge publishing account: shadowmaster404)
- **Upstream source:** https://github.com/shadowmaster435/Impactful_Weather
- **CurseForge project:** https://www.curseforge.com/minecraft/mc-mods/biome-particle-weather (project ID 575109)
- **Pinned commit:** `ee41b95e4f49c07bea0969062e1260428c499550` ("Updated to 1.20.1", 2023-10-06)
- **Original license:** Mozilla Public License 2.0 — full text at https://www.mozilla.org/MPL/2.0/

### License discrepancy (resolved)

The upstream root `LICENSE` is MPL-2.0, but upstream's own
`Forge/src/main/resources/META-INF/mods.toml` declares `license="GPLv3"` and the CurseForge
project page declares "GNU General Public License version 3 (GPLv3)". The two first-party
declarations conflict. We take the stricter declared terms: **the combined work Cold Snow is
GPL-3.0-only**, while the copied files below retain their MPL-2.0 notices and source
availability. If the upstream author clarifies that only MPL-2.0 applies, this notice will be
updated.

## MPL-2.0 covered files copied/modified by Cold Snow

All files in this section are subject to the Mozilla Public License, v. 2.0:

> This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
> If a copy of the MPL was not distributed with this file, You can obtain one at
> http://mozilla.org/MPL/2.0/

| Upstream path (at pinned commit) | Cold Snow path | Modifications |
|---|---|---|
| `Common/src/main/java/shadowmaster435/impactfulweather/client/particle/Snow.java` | `src/main/java/itamio/coldsnow/client/particle/derived/SnowParticle.java` | package/class rename; replaced `ClientConfig.INSTANCE.misc.snowspeedmodifier` with constructor parameter; removed static mutable `upv`/`cworld`; clamped `15f/light` colour divisor; factory renamed |
| `Common/src/main/java/shadowmaster435/impactfulweather/client/particle/BlizzardSnow.java` | `src/main/java/itamio/coldsnow/client/particle/derived/BlizzardSnowParticle.java` | package/class rename; removed static mutable `upv`/`cworld`; clamped colour divisor; factory renamed |
| `Common/src/main/java/shadowmaster435/impactfulweather/client/particle/BlizzardWind.java` | `src/main/java/itamio/coldsnow/client/particle/derived/BlizzardWindParticle.java` | package/class rename; removed static mutable `upv`; factory renamed |
| `Common/src/main/java/shadowmaster435/impactfulweather/mixin/client/LevelRendererMixin.java` | `src/main/java/itamio/coldsnow/mixin/LevelRendererMixin.java` | package rename; gated on synced Cold Snow storm stage; refmap regenerated |
| `Common/src/main/resources/assets/impactfulweather/particles/{snow,blizzardsnow,blizzardwind}.json` | `src/main/resources/assets/coldsnow/particles/*.json` | texture namespace `impactfulweather:` → `coldsnow:` |

### Textures (22 PNG files, unchanged bytes)

| Upstream path prefix | Cold Snow path prefix | Files |
|---|---|---|
| `Common/src/main/resources/assets/impactfulweather/textures/particle/` | `src/main/resources/assets/coldsnow/textures/particle/` | `snow_0.png` … `snow_6.png` (7) |
| same | same | `blizzardsnow_0.png` … `blizzardsnow_6.png` (7) |
| same | same | `blizzardwind_0.png` … `blizzardwind_7.png` (8) |

MPL-2.0 obligations honoured here: MPL notices retained in every copied source file; full MPL-2.0
text linked above and shipped as `MPL-2.0.txt`; modifications documented in this file; complete
source of the derived files is public in this repository; the combined work is GPL-3.0-only.

## Everything else

All other Cold Snow source, resources, and documentation are original work by Sekai0NI0itamio
and licensed GPL-3.0-only. Vanilla Minecraft assets are **not** redistributed.
