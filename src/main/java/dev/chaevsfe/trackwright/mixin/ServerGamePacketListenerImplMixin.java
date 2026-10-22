package dev.chaevsfe.trackwright.mixin;

import dev.chaevsfe.trackwright.track.WidestTurnFlag;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Inject(method = "handleUseItemOn", at = @At("HEAD"))
    private void trackwright$beforeUseOnBlock(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        WidestTurnFlag.beforeUseOnBlock();
    }

    @Inject(method = "handleUseItemOn", at = @At("RETURN"))
    private void trackwright$dropUnusedWidestTurn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        WidestTurnFlag.afterUseOnBlock(((ServerGamePacketListenerImpl) (Object) this).player, packet.getHand());
    }
}
