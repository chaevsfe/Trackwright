package dev.chaevsfe.trackwright.mixin;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import dev.chaevsfe.trackwright.track.CurveReach;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityConfigurationPacket.class)
public abstract class BlockEntityConfigurationPacketMixin {
    @Shadow
    @Final
    protected BlockPos pos;

    @Inject(method = "handle", at = @At("HEAD"))
    private void trackwright$measureCurve(ServerPlayer player, CallbackInfo ci) {
        if (player != null && (Object) this instanceof CurveReach curve) {
            curve.trackwright$measure(player.level(), pos);
        }
    }
}
