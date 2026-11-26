package itamio.coldsnow.client;

import itamio.coldsnow.Constants;
import itamio.coldsnow.config.ClientConfigData;
import itamio.coldsnow.core.StormSnapshot;
import itamio.coldsnow.core.StormStage;
import itamio.coldsnow.init.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Spawns Cold Snow particles around the camera while a storm stage is active. */
@EventBusSubscriber(modid = Constants.MODID, value = Dist.CLIENT)
public final class ColdSnowClient {
    private static final double SPAWN_RADIUS = 24.0;
    private static final double SPAWN_HEIGHT = 12.0;

    private ColdSnowClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }

        StormSnapshot snapshot = ClientStormState.get();
        StormStage stage = snapshot.stage();
        if (stage == null || stage == StormStage.CLEAR) {
            return;
        }

        ClientConfigData config = ClientConfigAccess.get();
        int flakes = ParticleSpawn.flakesFor(stage, snapshot.intensity(), config.particleDensity);
        if (flakes <= 0) {
            return;
        }

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        boolean blizzard = stage == StormStage.BLIZZARD;
        int snowCount = blizzard ? Math.max(1, (flakes + 1) / 2) : flakes;
        int windCount = blizzard && !config.disableWindTrails ? Math.max(1, flakes / 3) : 0;

        for (int i = 0; i < snowCount; i++) {
            double x = camera.x + (level.random.nextDouble() - 0.5) * SPAWN_RADIUS;
            double y = camera.y + level.random.nextDouble() * SPAWN_HEIGHT;
            double z = camera.z + (level.random.nextDouble() - 0.5) * SPAWN_RADIUS;
            level.addParticle(
                    blizzard ? ModParticles.BLIZZARD_FLAKE.get() : ModParticles.SNOW_FLAKE.get(),
                    x, y, z, 0.0, 0.0, 0.0);
        }
        for (int i = 0; i < windCount; i++) {
            double x = camera.x + (level.random.nextDouble() - 0.5) * SPAWN_RADIUS;
            double y = camera.y + level.random.nextDouble() * SPAWN_HEIGHT;
            double z = camera.z + (level.random.nextDouble() - 0.5) * SPAWN_RADIUS;
            level.addParticle(ModParticles.BLIZZARD_WIND.get(), x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
