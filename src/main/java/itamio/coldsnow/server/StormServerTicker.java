package itamio.coldsnow.server;

import itamio.coldsnow.Constants;
import itamio.coldsnow.config.ConfigStore;
import itamio.coldsnow.config.StormConfigData;
import itamio.coldsnow.core.StormSnapshot;
import itamio.coldsnow.core.StormStage;
import itamio.coldsnow.core.StormTimeline;
import itamio.coldsnow.core.StormTiming;
import itamio.coldsnow.network.ColdSnowNetwork;
import itamio.coldsnow.world.AccumulationService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/**
 * The single authority for storm weather. Runs on the Forge event bus and
 * only acts on the overworld, on the server, at the END of a level tick.
 */
@Mod.EventBusSubscriber(modid = Constants.MODID)
public final class StormServerTicker {
    private static final StormSnapshot CLEAR = new StormSnapshot(
            StormStage.CLEAR, -1, 0L, 0L, 0L, 0.0F);

    private static volatile StormConfigData cachedConfig;
    private static volatile AccumulationService cachedAccumulation;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END
                || event.side != LogicalSide.SERVER
                || !(event.level instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        StormStateData state = state(level);
        if (state == null) {
            return;
        }

        try {
            // One-time rule setup: vanilla piling is off, weather is ours.
            if (!state.rulesInitialized) {
                level.getGameRules().getRule(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT).set(0, server);
                // Mojang mappings name this rule RULE_WEATHER_CYCLE (gamerule "doWeatherCycle").
                level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
                state.rulesInitialized = true;
                state.setDirty();
            }

            StormConfigData config = config();
            StormSnapshot snapshot = snapshotFor(level, config);
            StormStage stage = snapshot == null ? StormStage.CLEAR : snapshot.stage();
            if (stage == null) {
                stage = StormStage.CLEAR;
            }
            StormSnapshot effective = snapshot == null ? CLEAR : snapshot;

            if (snapshot != null && state.cycleIndex != snapshot.cycleIndex()) {
                state.cycleIndex = snapshot.cycleIndex();
                state.setDirty();
            }

            // Only touch vanilla weather on an actual stage transition.
            if (state.lastStage != stage) {
                applyWeather(level, effective);
                state.lastStage = stage;
                state.setDirty();
                ColdSnowNetwork.sendToAll(level, effective);
            }

            if (config != null && config.enabled) {
                accumulation(config).tick(level, effective);
            } else {
                // Disabled config: keep melting toward a clear column.
                accumulation(config).tick(level, new StormSnapshot(StormStage.CLEAR,
                        effective.cycleIndex(), effective.dayTimeTicks(), 0L, 0L, 0.0F));
            }
        } catch (RuntimeException ignored) {
            // Fail open: a bad tick must never break the server loop.
        }
    }

    /** A joining client needs the current state immediately. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level;
        try {
            level = player.serverLevel();
        } catch (RuntimeException ignored) {
            return;
        }
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }
        try {
            ColdSnowNetwork.sendToAll(level, snapshotFor(level, config()));
        } catch (RuntimeException ignored) {
            // Networking is best-effort.
        }
    }

    private static void applyWeather(ServerLevel level, StormSnapshot snapshot) {
        if (level == null || snapshot == null) {
            return;
        }
        if (snapshot.stage() == StormStage.CLEAR) {
            level.setWeatherParameters(0, 0, false, false);
            return;
        }
        long remaining = snapshot.stageLengthTicks() - snapshot.tickInStage();
        int ticks = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, remaining));
        level.setWeatherParameters(0, ticks, true, false);
    }

    private static StormSnapshot snapshotFor(ServerLevel level, StormConfigData config) {
        if (level == null || config == null || !config.enabled) {
            return CLEAR;
        }
        try {
            StormTiming timing = new StormTiming(config.cycleDaysMin, config.cycleDaysMax, config.seed);
            StormSnapshot snapshot = new StormTimeline(timing).at(level.getDayTime());
            return snapshot == null ? CLEAR : snapshot;
        } catch (RuntimeException ignored) {
            return CLEAR;
        }
    }

    private static StormStateData state(ServerLevel level) {
        try {
            return StormStateData.getOrCreate(level);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static StormConfigData config() {
        StormConfigData local = cachedConfig;
        if (local != null) {
            return local;
        }
        synchronized (StormServerTicker.class) {
            if (cachedConfig == null) {
                StormConfigData loaded;
                try {
                    Path path = FMLPaths.CONFIGDIR.get().resolve(Constants.MODID + ".json");
                    ConfigStore<StormConfigData> store =
                            new ConfigStore<>(path, StormConfigData.class, StormConfigData::new);
                    StormConfigData raw = store.load();
                    loaded = (raw == null ? new StormConfigData() : raw).validate();
                } catch (RuntimeException | LinkageError ignored) {
                    loaded = new StormConfigData().validate();
                }
                cachedConfig = loaded == null ? new StormConfigData().validate() : loaded;
            }
            return cachedConfig;
        }
    }

    private static AccumulationService accumulation(StormConfigData config) {
        AccumulationService local = cachedAccumulation;
        if (local != null) {
            return local;
        }
        synchronized (StormServerTicker.class) {
            if (cachedAccumulation == null) {
                int layers = config == null ? 16 : config.maxLayers;
                int interval = config == null ? 20 : config.accumulationIntervalTicks;
                cachedAccumulation = new AccumulationService(layers, interval);
            }
            return cachedAccumulation;
        }
    }

    private StormServerTicker() {
        throw new AssertionError("No instances");
    }
}
