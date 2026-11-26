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
 * <p>Adaptations: the upstream static mutable {@code upv}/{@code cworld} fields are gone;
 * the {@code ClientConfig.INSTANCE.misc.snowspeedmodifier} dependency is now an explicit
 * constructor {@code speed} parameter; the {@code 15f / light} divisor is clamped so a dark
 * or invalid light sample can never divide by zero.</p>
 */
public class SnowParticle extends SimpleAnimatedParticle {
    /** Neutral replacement for the upstream config-driven fall speed. */
    public static final float DEFAULT_SPEED = 1.0f;
    /** Neutral replacement for the upstream statically-shared "up" value. */
    public static final float DEFAULT_UP = 0.0f;

    private final float speed;
    public float light;
    public float groundtimer = 5f;

    public SnowParticle(ClientLevel world, double x, double y, double z, SpriteSet sprites, float up, float speed) {
        super(world, x, y, z, sprites, up);
        this.speed = Math.abs(Float.isFinite(speed) ? speed : DEFAULT_SPEED);
        this.xd = 0.0D;
        this.yd = -3;
        this.zd = 0.0D;
        this.gravity = 0f;
        this.quadSize = 0.125f;
        this.alpha = 0f;
        this.setSize(0.01F, 0.01F);
        this.setSprite(sprites.get(world.random));
    }

    /** Never lets the light sample reach zero (or become NaN) before dividing. */
    private static float shade(float lightLevel) {
        float value = Float.isFinite(lightLevel) ? Math.max(lightLevel, 0.01f) : 0.01f;
        return 15f / value;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        BlockPos pos = new BlockPos((int) this.x, (int) this.y, (int) this.z);
        this.light = this.level.getLightLevelDependentMagicValue(pos) + 0.01f;
        float colour = shade(this.light);
        this.setColor(colour, colour, colour);
        ++this.age;
        if (this.age < 10) {
            this.alpha = this.age / 10f;
        } else {
            this.alpha = 1f;
        }
        if (this.onGround
                || this.level.getFluidState(pos).is(FluidTags.WATER)
                || this.level.getFluidState(pos).is(FluidTags.LAVA)) {
            this.groundtimer = this.groundtimer - 1;
            this.yd = 0;
            this.quadSize = this.quadSize - 0.035f;
            if (this.groundtimer <= 0 || this.level.getBlockState(pos).blocksMotion()) {
                this.remove();
            }
        } else {
            this.groundtimer = 5;
            this.quadSize = 0.175f;
            this.xd = 0;
            this.zd = 0;
            this.yd = -Math.max(this.speed, 0.01f);
        }
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;
        private final float speed;

        public Factory(SpriteSet sprites) {
            this(sprites, DEFAULT_SPEED);
        }

        public Factory(SpriteSet sprites, float speed) {
            this.spriteProvider = sprites;
            this.speed = speed;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z,
                                       double Xv, double Yv, double Zv) {
            SnowParticle particle = new SnowParticle(world, x, y, z, this.spriteProvider, DEFAULT_UP, this.speed);
            particle.pickSprite(this.spriteProvider);
            return particle;
        }
    }
}
