package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.trains.track.CurvedTrackDestroyPacket;
import dev.chaevsfe.trackwright.track.ConnectionLength;
import dev.chaevsfe.trackwright.track.CurveReach;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CurvedTrackDestroyPacket.class)
public abstract class CurvedTrackDestroyPacketMixin implements CurveReach {
    @Shadow
    @Final
    private BlockPos targetPos;

    @Unique
    private int trackwright$reach;

    @Override
    public void trackwright$measure(Level level, BlockPos pos) {
        trackwright$reach = ConnectionLength.curveLength(level, pos, targetPos);
    }

    @ModifyExpressionValue(
        method = {"maxRange", "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/content/trains/track/TrackBlockEntity;)V"},
        at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;"),
        require = 2,
        allow = 2
    )
    private Object trackwright$reachAlongDestroyedCurve(Object value) {
        return ConnectionLength.alongCurve(value, trackwright$reach);
    }
}
