package itamio.coldsnow.init;

import itamio.coldsnow.Constants;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Server-side-safe registration of the three particle types used by the
 * ported particle providers. The registry names intentionally match the
 * {@code assets/coldsnow/particles/<name>.json} files shipped with the mod.
 */
public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Constants.MODID);

    public static final RegistryObject<SimpleParticleType> SNOW_FLAKE =
            PARTICLES.register("snow", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> BLIZZARD_FLAKE =
            PARTICLES.register("blizzardsnow", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> BLIZZARD_WIND =
            PARTICLES.register("blizzardwind", () -> new SimpleParticleType(true));

    /** Attach the deferred register to the mod event bus during mod construction. */
    public static void register(IEventBus bus) {
        if (bus != null) {
            PARTICLES.register(bus);
        }
    }

    private ModParticles() {
        throw new AssertionError("No instances");
    }
}
