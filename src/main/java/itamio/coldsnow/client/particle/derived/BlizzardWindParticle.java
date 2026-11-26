/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/
 */
package itamio.coldsnow.client.particle.derived;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;

/**
 * Ported from upstream Impactful Weather (MPL-2.0), pinned commit
 * {@code ee41b95e4f49c07bea0969062e1260428c499550}.
 *
 * <p>Adaptation: the upstream static mutable {@code upv} field is gone; the sprite animation,
 * translucent billboard rendering and fade-in/fade-out are unchanged from upstream.</p>
 */
public class BlizzardWindParticle extends SimpleAnimatedParticle {
    /** Neutral replacement for the upstream statically-shared "up" value. */
    public static final float DEFAULT_UP = 0.0f;

    public float light;

    public BlizzardWindParticle(ClientLevel world, double x, double y, double z, SpriteSet sprites, float up) {
        super(world, x, y, z, sprites, up);
        this.xd = 0.0D;
        this.yd = 0;
        this.zd = 0.0D;
        this.lifetime = (int) (15 + Math.floor(Math.random() * 5));
        this.gravity = 0f;
        this.alpha = 0.0f;
        this.quadSize = 0.0f;
        this.age = 0;
        this.quadSize = (float) (Math.random() / 2) + 0.2f;
        this.setSpriteFromAge(this.sprites);
        this.setSize(0.02F, 0.02f);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        BlockPos pos = new BlockPos((int) this.x, (int) this.y, (int) this.z);

        if (this.age > this.lifetime
                || this.level.getBlockState(pos).blocksMotion()
                || this.level.getFluidState(pos).is(FluidTags.WATER)
                || this.level.getFluidState(pos).is(FluidTags.LAVA)) {
            this.remove();
            this.age = 0;
        } else {
            ++this.age;
            this.setSpriteFromAge(this.sprites);
            if (this.age < 5) {
                this.alpha = this.alpha + 0.08f;
            } else {
                this.alpha = 0.4f;
            }
            if (this.age >= 10) {
                this.alpha = this.alpha - 0.08f;
            } else {
                this.alpha = 0.4f;
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;

        public Factory(SpriteSet sprites) {
            this.spriteProvider = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z,
                                       double Xv, double Yv, double Zv) {
            BlizzardWindParticle particle =
                    new BlizzardWindParticle(world, x, y, z, this.spriteProvider, DEFAULT_UP);
            particle.pickSprite(this.spriteProvider);
            return particle;
        }
    }
}
