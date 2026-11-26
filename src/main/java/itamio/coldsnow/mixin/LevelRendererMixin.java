/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/
 */
package itamio.coldsnow.mixin;

import itamio.coldsnow.client.ClientStormState;
import itamio.coldsnow.core.StormStage;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses vanilla weather rendering while a Cold Snow stage owns the weather.
 *
 * <p>Ported from upstream Impactful Weather (MPL-2.0), pinned commit
 * {@code ee41b95e4f49c07bea0969062e1260428c499550}. Uses non-destructive
 * {@link Inject} cancellation instead of {@code @Overwrite}, and only cancels when a Cold
 * Snow stage is active; the mixin config is {@code required: false} so failure to apply is
 * fail-open.</p>
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    /** Matches 1.20.1 {@code LevelRenderer.renderSnowAndRain(LightTexture,float,double,double,double)}. */
    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void coldsnow$renderSnowAndRain(LightTexture manager, float partialTicks,
                                            double x, double y, double z, CallbackInfo ci) {
        if (ClientStormState.get().stage() != StormStage.CLEAR) {
            ci.cancel();
        }
    }

    /** Matches 1.20.1 {@code LevelRenderer.tickRain(Camera)}. */
    @Inject(method = "tickRain", at = @At("HEAD"), cancellable = true)
    private void coldsnow$tickRain(Camera camera, CallbackInfo ci) {
        if (ClientStormState.get().stage() != StormStage.CLEAR) {
            ci.cancel();
        }
    }
}
