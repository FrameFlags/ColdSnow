# Cold Snow — implementation contract (v1)

Target: Minecraft **1.20.1 Forge**, Java **17**, Forge **47.4.20**, ForgeGradle **6.0.54**,
Gradle **8.14.5**, official Mojang mappings, `pack_format 15`.
Mod id: `coldsnow`. Package root: `itamio.coldsnow`.
License: **GPL-3.0-only** overall. Three ported particle files + their PNGs keep upstream MPL-2.0 notices.

Build rule: **never build locally**. All compile/test runs happen in GitHub Actions.
A checkbox is true only with a CI run URL or file evidence.

## File ownership (do not edit outside your set)

- `core`/`config`/tests → `src/main/java/itamio/coldsnow/core/**`, `src/main/java/itamio/coldsnow/config/**`, `src/test/java/**`
- server/network/world/entry → `src/main/java/itamio/coldsnow/ColdSnowMod.java`, `src/main/java/itamio/coldsnow/{init,server,network,world}/**`
- client/particles/mixin/assets → `src/main/java/itamio/coldsnow/client/**`, `src/main/java/itamio/coldsnow/mixin/**`, `src/main/resources/assets/**`, `src/main/resources/coldsnow.mixins.json`
- build/CI/docs → root build files, `.github/**`, `*.md`, `LICENSE*`, `THIRD_PARTY_NOTICES.md`

## Exact API (must match, so the modules compile together)

### core (PURE JAVA — zero `net.minecraft` imports)
```java
package itamio.coldsnow.core;
public enum StormStage { CLEAR, LIGHT, HEAVY, BLIZZARD, TAPER }
public record StormSnapshot(StormStage stage, int cycleIndex, long dayTimeTicks,
                            long tickInStage, long stageLengthTicks, float intensity) {}
public record StormTiming(int minDays, int maxDays, long seed) {
    public static StormTiming defaults();               // 14, 20, 42L
    public StormTiming { minDays = Math.max(1, Math.min(minDays, maxDays)); }
}
public final class StormTimeline {
    public static final long DAY_TICKS = 24000L;
    public StormTimeline(StormTiming timing);
    public long cycleLengthTicks(int cycleIndex);       // (minDays + hash(seed,i) % (maxDays-minDays+1)) * DAY_TICKS
    public long cycleStartTicks(int cycleIndex);
    public StormSnapshot at(long dayTimeTicks);         // pure function of (timing, dayTime); handles negative
}
public final class SnowDepthPolicy {
    public int targetLayers(StormSnapshot snapshot, int maxLayers); // 0..maxLayers, clamped
    public int step(int currentLayers, int targetLayers);           // -1 / 0 / +1
}
```
Stage fractions inside a cycle: LIGHT [0,.25), HEAVY [.25,.55), BLIZZARD [.55,.85), TAPER [.85,1).
Intensity: LIGHT .25→.5, HEAVY .5→.85, BLIZZARD .85→1, TAPER 1→0.
Look up cycle by bounded search from an average-length guess (max 128 adjustments), never by dividing by a variable length.

### config (PURE POJO + Gson; zero `net.minecraft` imports)
```java
package itamio.coldsnow.config;
public final class StormConfigData {                    // config/coldsnow.json (server)
    public int schemaVersion = 1;
    public boolean enabled = true;
    public int cycleDaysMin = 14, cycleDaysMax = 20;
    public long seed = 42L;
    public int maxLayers = 16;                          // clamped to [8,64]
    public int accumulationIntervalTicks = 20;
    public StormConfigData validate();                  // clamps, returns this
}
public final class ClientConfigData {                   // config/coldsnow-client.json (client)
    public int schemaVersion = 1;
    public float particleDensity = 1.0f;                // clamp [0,2]
    public boolean respectVanillaParticleSetting = true;
    public boolean disableWindTrails = false;
    public boolean disableScreenFlash = true;
    public ClientConfigData validate();
}
public final class ConfigStore<T> {
    public ConfigStore(java.nio.file.Path path, Class<T> type, java.util.function.Supplier<T> defaults);
    public T load();                                    // malformed -> .invalid backup + defaults
    public void save(T value);                          // atomic temp + move
}
```
Use Gson with `setPrettyPrinting()`. `validate()` must clamp; never throw on bad values.

### init (server-side registration, Forge DeferredRegister)
```java
package itamio.coldsnow.init;
public final class ModParticles {
    public static final RegistryObject<SimpleParticleType> SNOW_FLAKE;
    public static final RegistryObject<SimpleParticleType> BLIZZARD_FLAKE;
    public static final RegistryObject<SimpleParticleType> BLIZZARD_WIND;
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus);
}
```

### server / network / world
```java
public final class StormStateData extends SavedData {   // key "coldsnow-storm", OVERWORLD only
    public int cycleIndex = -1;
    public StormStage lastStage = StormStage.CLEAR;
    public boolean rulesInitialized = false;
    public static StormStateData create();
    public static StormStateData load(CompoundTag tag);
    public CompoundTag save(CompoundTag tag);
}
public final class AccumulationService {                // no custom block
    public AccumulationService(int maxLayers, int intervalTicks);
    public void tick(ServerLevel level, StormSnapshot snapshot);
}
public final class ColdSnowNetwork {
    public static void register();
    public static void sendToAll(ServerLevel level, StormSnapshot snapshot);
}
public record StormStatePacket(StormStage stage, float intensity) { /* encode/decode/handle */ }
```
`ColdSnowMod`: `@Mod(MODID)`; register DeferredRegisters + `ColdSnowNetwork.register()` on `FMLCommonSetupEvent`
(via `enqueueWork`). Use `Constants.MODID` in `itamio.coldsnow.Constants`.
Server tick hook: `@EventBusSubscriber(modid = Constants.MODID)` class `StormServerTicker`,
`@SubscribeEvent static void onLevelTick(TickEvent.LevelTickEvent e)` with `e.phase == END`,
`e.side == LogicalSide.SERVER`, `e.level instanceof ServerLevel level`, `level.dimension() == Level.OVERWORLD`.
Once: `getGameRules().getRule(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT).set(0, server)` and
`RULE_DO_WEATHER_CYCLE.set(false, server)`. Call `setWeatherParameters(0, remainingTicks, true, false)`
ONLY on stage transitions; on CLEAR call `setWeatherParameters(0, 0, false, false)`.
Persist `cycleIndex`/`lastStage` in `SavedData`. Send `StormStatePacket` on transitions and on player login.

### client
```java
public final class ClientStormState {
    public static void set(StormSnapshot s);
    public static StormSnapshot get();                  // never null; CLEAR default
}
@EventBusSubscriber(modid = Constants.MODID, value = Dist.CLIENT)
public final class ColdSnowClient {
    @SubscribeEvent static void onClientTick(TickEvent.ClientTickEvent e);
}
public final class ParticleProviders {
    @SubscribeEvent public static void onRegister(RegisterParticleProvidersEvent e); // mod bus
}
public final class ParticleSpawn {                       // pure-ish mapping, unit-testable
    public static int flakesFor(StormStage stage, float intensity, float densityMultiplier);
}
```
Client tick: spawn via `ClientLevel.addParticle(type, x,y,z, 0,0,0)` around the camera only when
`ClientStormState.get().stage() != CLEAR`; respect density multiplier. Particle classes live in
`itamio.coldsnow.client.particle.derived` with upstream MPL headers.

## Vanilla facts enforced
- `SnowLayerBlock.LAYERS` is 1..8; 9 causes AIOOBE and has no model. Never widen it.
- Accumulation is in `ServerLevel.tickChunk`; we disable it with gamerule `RULE_SNOW_ACCUMULATION_HEIGHT=0`
  and re-implement it in `AccumulationService` with `biome.shouldSnow(level,pos)` + `Block.pushEntitiesUp`.
- Depth is `Blocks.SNOW` LAYERS 1..8, then `Blocks.SNOW_BLOCK` full cubes; lower full cubes satisfy
  `SnowLayerBlock.canSurvive`. Replacing an 8-layer SNOW with SNOW_BLOCK is layer-neutral — place a new
  layer on top to gain the next layer.
- `ClientLevel.addParticle` works client-side; `Level.addParticle` is a no-op on the server.
- 1 day = 24,000 ticks; 14–20 days = 336,000–480,000 ticks (fits int).

## Licensing
- Copy `Snow.java`, `BlizzardSnow.java`, `BlizzardWind.java` from upstream master commit
  `ee41b95e4f49c07bea0969062e1260428c499550`, keep MPL-2.0 headers, rename package/class,
  remove `ClientConfig.INSTANCE` dependency, remove static mutable `upv`/`cworld`, clamp `15f/light`.
- 22 upstream PNGs (snow_0-6, blizzardsnow_0-6, blizzardwind_0-7) ship with notices.
- `mods.toml` license = `GPL-3.0-only`; `THIRD_PARTY_NOTICES.md` records upstream MPL-2.0 +
  mods.toml/CurseForge GPLv3 conflict and the resolution.
