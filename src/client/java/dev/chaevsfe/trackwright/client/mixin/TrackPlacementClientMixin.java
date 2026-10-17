package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.catnip.data.Couple;
import com.zurrtum.create.client.content.trains.track.TrackPlacementClient;
import com.zurrtum.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.client.config.ClientConfig;
import dev.chaevsfe.trackwright.client.guide.AimHelp;
import dev.chaevsfe.trackwright.client.guide.TrackGuide;
import dev.chaevsfe.trackwright.client.guide.WidestTurnLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(TrackPlacementClient.class)
public abstract class TrackPlacementClientMixin {
    @Inject(method = "clientTick", at = @At("HEAD"))
    private static void trackwright$forgetLastPlan(Minecraft mc, CallbackInfo ci) {
        TrackGuide.forget();
    }

    @ModifyExpressionValue(
        method = "clientTick",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;", opcode = Opcodes.GETFIELD)
    )
    private static @Nullable HitResult trackwright$aim(@Nullable HitResult hit, @Local(argsOnly = true) Minecraft mc) {
        return AimHelp.aim(mc, hit);
    }

    @ModifyExpressionValue(
        method = "clientTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/BlockHitResult;getDirection()Lnet/minecraft/core/Direction;", ordinal = 1)
    )
    private static Direction trackwright$squaresOnceSettled(Direction face) {
        return AimHelp.faceForSquares(face);
    }

    @WrapOperation(
        method = "clientTick",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/catnip/data/Couple;either(Ljava/util/function/Predicate;)Z")
    )
    private static boolean trackwright$alwaysShowSquares(Couple<?> hints, Predicate<?> empty, Operation<Boolean> original) {
        return !ClientConfig.get().alwaysShowPlacementSquares() && original.call(hints, empty);
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
