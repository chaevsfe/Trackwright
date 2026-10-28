package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.content.trains.track.TrackVisual;
import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import dev.chaevsfe.trackwright.client.track.FarEnd;
import dev.chaevsfe.trackwright.client.track.VisualTrack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrackVisual.class)
public abstract class TrackVisualMixin implements VisualTrack {
    @Shadow
    @Final
    protected TrackBlockEntity blockEntity;

    @Override
    public TrackBlockEntity trackwright$track() {
        return blockEntity;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void trackwright$visualCreated(CallbackInfo ci) {
        FarEnd.visualCreated(blockEntity);
    }

    @WrapOperation(
        method = "createInstance",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/trains/track/BezierConnection;isPrimary()Z")
    )
    private boolean trackwright$drawFromFarEnd(BezierConnection connection, Operation<Boolean> original) {
        return original.call(connection) || FarEnd.firstEndWithoutVisual(blockEntity.getLevel(), connection);
    }
}
