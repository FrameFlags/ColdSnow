package itamio.coldsnow.client;

import itamio.coldsnow.Constants;
import itamio.coldsnow.client.particle.derived.BlizzardSnowParticle;
import itamio.coldsnow.client.particle.derived.BlizzardWindParticle;
import itamio.coldsnow.client.particle.derived.SnowParticle;
import itamio.coldsnow.init.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Binds the three {@link ModParticles} types to the ported sprite-set factories.
 * Registered on the mod event bus for the client side only.
 */
@EventBusSubscriber(modid = Constants.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ParticleProviders {
    private ParticleProviders() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SNOW_FLAKE.get(), SnowParticle.Factory::new);
        event.registerSpriteSet(ModParticles.BLIZZARD_FLAKE.get(), BlizzardSnowParticle.Factory::new);
        event.registerSpriteSet(ModParticles.BLIZZARD_WIND.get(), BlizzardWindParticle.Factory::new);
    }
}
