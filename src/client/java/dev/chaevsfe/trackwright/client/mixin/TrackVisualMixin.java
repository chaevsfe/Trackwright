package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackVisual;
import dev.chaevsfe.trackwright.client.track.FarEnd;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TrackVisual.class)
public abstract class TrackVisualMixin {
    @Shadow
    @Final
    protected TrackBlockEntity blockEntity;

    @WrapOperation(
        method = "createInstance",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/track/BezierConnection;isPrimary()Z")
    )
    private boolean trackwright$drawFromFarEnd(BezierConnection connection, Operation<Boolean> original) {
        return original.call(connection) || FarEnd.firstEndUnloaded(blockEntity.getLevel(), connection);
    }
}
