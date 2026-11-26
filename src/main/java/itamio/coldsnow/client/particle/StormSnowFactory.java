package itamio.coldsnow.client.particle;

import itamio.coldsnow.client.ClientStormState;
import itamio.coldsnow.client.particle.derived.SnowParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Cold Snow's original factory for light/heavy snow. The ported upstream particle
 * exposes a fall-speed parameter; this factory feeds it from the server-synced
 * storm intensity so flakes genuinely fall faster as the storm builds.
 *
 * <p>MPL-2.0 note: this file is original Cold Snow code (GPL-3.0-only); the
 * {@link SnowParticle} class it instantiates remains MPL-2.0 with its own notice.</p>
 */
public final class StormSnowFactory implements ParticleProvider<SimpleParticleType> {

    private final SpriteSet sprites;

    public StormSnowFactory(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                   double x, double y, double z,
                                   double vx, double vy, double vz) {
        float intensity = ClientStormState.get().intensity();
        if (!Float.isFinite(intensity)) {
            intensity = 0.0f;
        }
        intensity = Math.max(0.0f, Math.min(1.0f, intensity));

        // 0.6 at the start of light snow, up to ~2.4 at the peak of TAPER/BLIZZARD.
        float speed = 0.6f + intensity * 1.8f;
        SnowParticle particle = new SnowParticle(
                level, x, y, z, this.sprites, SnowParticle.DEFAULT_UP, speed);
        particle.pickSprite(this.sprites);
        return particle;
    }
}
