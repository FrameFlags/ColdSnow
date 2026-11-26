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
 * <p>Adaptations: the upstream static mutable {@code upv}/{@code cworld} fields are gone and
 * the {@code 15f / light} divisor is clamped. The horizontal blizzard drift, lifetime, alpha
 * fade and ground shrink are unchanged from upstream.</p>
 */
public class BlizzardSnowParticle extends SimpleAnimatedParticle {
    /** Neutral replacement for the upstream statically-shared "up" value. */
    public static final float DEFAULT_UP = 0.0f;

    public float light;
    public float groundtimer = 5f;

    public BlizzardSnowParticle(ClientLevel world, double x, double y, double z, SpriteSet sprites, float up) {
        super(world, x, y, z, sprites, up);
        this.xd = 0.0D;
        this.yd = -3;
        this.zd = 0.0D;
        this.gravity = 0f;
        this.quadSize = 0.125f;
        this.alpha = 0f;
        this.lifetime = 40;
        this.setSize(0.01F, 0.01F);
        this.setSprite(sprites.get(world.random));
        this.light = world.getLightLevelDependentMagicValue(
                new BlockPos((int) this.x, (int) this.y, (int) this.z)) + 0.01f;
        float colour = shade(this.light);
        this.setColor(colour, colour, colour);
    }

    /** Never lets the light sample reach zero (or become NaN) before dividing. */
    private static float shade(float lightLevel) {
        float value = Float.isFinite(lightLevel) ? Math.max(lightLevel, 0.01f) : 0.01f;
        return 15f / value;
    }

    @Override
    public void tick() {
        this.light = this.level.getLightLevelDependentMagicValue(
                new BlockPos((int) this.x, (int) this.y, (int) this.z)) + 0.01f;
        float colour = shade(this.light);
        this.setColor(colour, colour, colour);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        BlockPos pos = new BlockPos((int) this.x, (int) this.y, (int) this.z);

        if (this.onGround
                || this.level.getBlockState(pos).blocksMotion()
                || this.level.getFluidState(pos).is(FluidTags.WATER)
                || this.level.getFluidState(pos).is(FluidTags.LAVA)) {
            this.groundtimer = this.groundtimer - 1;
            this.yd = 0;
            this.quadSize = this.quadSize - 0.035f;
            if (this.groundtimer <= 0) {
                this.remove();
            }
        } else {
            ++this.age;
            if (this.age >= this.lifetime) {
                this.remove();
            }
            this.groundtimer = 5;
            this.quadSize = 0.175f;
            this.xd = 2;
            this.zd = 0;
            this.yd = 0;
        }
        if (this.age < 10) {
            this.alpha = this.age / 10f;
        } else if (this.age > (this.lifetime - 10)) {
            this.alpha = 1f - ((this.age - (this.lifetime - 10)) / 10f);
        } else {
            this.alpha = 1f;
        }
        this.move(this.xd, this.yd, this.zd);
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
            BlizzardSnowParticle particle =
                    new BlizzardSnowParticle(world, x, y, z, this.spriteProvider, DEFAULT_UP);
            particle.pickSprite(this.spriteProvider);
            return particle;
        }
    }
}
