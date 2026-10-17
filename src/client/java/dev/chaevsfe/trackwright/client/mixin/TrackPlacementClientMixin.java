package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.trains.track.TrackPlacementClient;
import com.zurrtum.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.client.guide.TrackGuide;
import dev.chaevsfe.trackwright.client.guide.WidestTurnLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrackPlacementClient.class)
public abstract class TrackPlacementClientMixin {
    @Inject(method = "clientTick", at = @At("HEAD"))
    private static void trackwright$forgetLastPlan(Minecraft mc, CallbackInfo ci) {
        TrackGuide.forget();
    }

    @ModifyExpressionValue(method = "clientTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private static boolean trackwright$lockedWidestTurn(boolean held) {
        return held || WidestTurnLock.locked();
    }

    @ModifyExpressionValue(method = "sendExtenderPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private static boolean trackwright$lockedWidestTurnOnPlace(
        boolean held,
        @Local(argsOnly = true) LocalPlayer player,
        @Local(argsOnly = true) InteractionHand hand
    ) {
        return held || WidestTurnLock.appliesToPlacement(player, hand);
    }

    @WrapOperation(
        method = "clientTick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/zurrtum/create/content/trains/track/TrackPlacement;tryConnect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/ItemStack;ZZ)Lcom/zurrtum/create/content/trains/track/TrackPlacement$PlacementInfo;",
            ordinal = 0
        )
    )
    private static TrackPlacement.PlacementInfo trackwright$planForGuide(
        Level level,
        Player player,
        BlockPos target,
        BlockState state,
        ItemStack stack,
        boolean girder,
        boolean maximiseTurn,
        Operation<TrackPlacement.PlacementInfo> original
    ) {
        TrackPlacement.PlacementInfo info = original.call(level, player, target, state, stack, girder, maximiseTurn);
        TrackGuide.capture(level, player, target, state, stack, maximiseTurn, info);
        return info;
    }
}
