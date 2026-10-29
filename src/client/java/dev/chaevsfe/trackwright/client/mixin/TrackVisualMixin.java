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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrackVisual.class)
public abstract class TrackVisualMixin {
    @Shadow
    @Final
    protected TrackBlockEntity blockEntity;

    @Unique
    private boolean trackwright$updating;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void trackwright$visualCreated(CallbackInfo ci) {
        FarEnd.visualCreated(blockEntity);
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void trackwright$updateStarts(float partialTick, CallbackInfo ci) {
        trackwright$updating = true;
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void trackwright$updateEnds(float partialTick, CallbackInfo ci) {
        trackwright$updating = false;
    }

    @Inject(method = "_delete", at = @At("HEAD"))
    private void trackwright$visualDeleted(CallbackInfo ci) {
        if (!trackwright$updating) {
            FarEnd.visualDeleted(blockEntity);
        }
    }

    @WrapOperation(
        method = "createInstance",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/track/BezierConnection;isPrimary()Z")
    )
    private boolean trackwright$drawFromFarEnd(BezierConnection connection, Operation<Boolean> original) {
        return original.call(connection) || FarEnd.firstEndWithoutVisual(blockEntity.getLevel(), connection);
    }
}
