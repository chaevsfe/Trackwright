package dev.chaevsfe.trackwright.client.mixin;

import dev.chaevsfe.trackwright.client.track.FarEnd;
import dev.chaevsfe.trackwright.client.track.VisualTrack;
import dev.engine_room.flywheel.lib.visual.AbstractVisual;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVisual.class)
public abstract class AbstractVisualMixin {
    @Shadow
    protected boolean deleted;

    @Inject(method = "delete", at = @At("HEAD"))
    private void trackwright$visualDeleted(CallbackInfo ci) {
        if (!deleted && (Object) this instanceof VisualTrack track) {
            FarEnd.visualDeleted(track.trackwright$track());
        }
    }
}
