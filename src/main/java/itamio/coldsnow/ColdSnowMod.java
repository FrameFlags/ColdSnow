package itamio.coldsnow;

import itamio.coldsnow.init.ModParticles;
import itamio.coldsnow.network.ColdSnowNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Entry point. Particle deferred registers are attached to the mod bus during
 * construction; the network channel is registered during common setup on the
 * main thread via {@code enqueueWork}.
 *
 * <p>The Forge-bus subscribers ({@code StormServerTicker}) are discovered by
 * {@code @EventBusSubscriber} and need no manual registration here.</p>
 */
@Mod(Constants.MODID)
public final class ColdSnowMod {
    public ColdSnowMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModParticles.register(modBus);
        modBus.addListener(ColdSnowMod::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ColdSnowNetwork::register);
    }
}
