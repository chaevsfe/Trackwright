package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.trains.track.TrackPlacementOverlay;
import dev.chaevsfe.trackwright.client.guide.WidestTurnLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TrackPlacementOverlay.class)
public abstract class TrackPlacementOverlayMixin {
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean trackwright$lockedWidestTurn(boolean held) {
        return held || WidestTurnLock.locked();
    }
}
