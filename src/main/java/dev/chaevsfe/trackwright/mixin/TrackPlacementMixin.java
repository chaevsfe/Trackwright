package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.simibubi.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.track.ConnectionLength;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import dev.chaevsfe.trackwright.track.PlacementRules;
import dev.chaevsfe.trackwright.track.UnloadedStart;
import dev.chaevsfe.trackwright.undo.UndoRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrackPlacement.class)
public abstract class TrackPlacementMixin {
    @ModifyExpressionValue(
        method = "tryConnect",
        at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;")
    )
    private static Object trackwright$longerConnections(Object value, @Local(argsOnly = true) Level level) {
        return ConnectionLength.forPlacement(value, level);
    }

    @ModifyExpressionValue(
        method = "tryConnect",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;equal(DD)Z", ordinal = 4)
    )
    private static boolean trackwright$bridgeBlockedStraight(
        boolean flat,
        @Local(name = "level") Level level,
        @Local(name = "info") TrackPlacement.PlacementInfo info,
        @Local(argsOnly = true) Player player,
        @Local(argsOnly = true) BlockState state2,
        @Local(name = "pos1") BlockPos pos1,
        @Local(name = "axis1") Vec3 axis1,
        @Local(name = "axis2") Vec3 axis2,
        @Local(name = "skipCurve") LocalBooleanRef skipCurve
    ) {
        if (!flat || !skipCurve.get()) {
            return flat;
        }
        PlacementRules.Straight straight = PlacementRules.decideStraight(level, player, pos1, state2, axis1, axis2, info.end1Extent);
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        extension.trackwright$setBoreAccess(straight.access());
        if (straight.unseen()) {
            extension.trackwright$setGuessed(true);
        }
        if (straight.bore() != null) {
            extension.trackwright$setBore(straight.bore());
            if (straight.bore().guessed()) {
                extension.trackwright$setGuessed(true);
            }
        } else if (straight.bridge()) {
            info.end1Extent = 0;
            info.end2Extent = 0;
            skipCurve.set(false);
            extension.trackwright$setBridged(true);
            if (level.isClientSide()) {
                extension.trackwright$setBridgedCells(straight.blocked());
            }
        }
        return flat;
    }

    @WrapOperation(
        method = "tryConnect",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;")
    )
    private static BlockState trackwright$previewFromUnloadedStart(
        Level level,
        BlockPos pos,
        Operation<BlockState> original,
        @Local(argsOnly = true) ItemStack stack,
        @Local(name = "info") TrackPlacement.PlacementInfo info
    ) {
        BlockState found = original.call(level, pos);
        BlockState state = UnloadedStart.startState(level, pos, found, stack);
        if (state != found) {
            ((PlacementInfoExtension) info).trackwright$setGuessed(true);
        }
        return state;
    }

    @WrapOperation(
        method = "placeTracks",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", ordinal = 0)
    )
    private static BlockState trackwright$countUnloadedStartAsTrack(
        Level level,
        BlockPos pos,
        Operation<BlockState> original,
        @Local(name = "first") boolean firstEnd,
        @Local(name = "pos") BlockPos start,
        @Local(argsOnly = true, ordinal = 0) BlockState startState
    ) {
        return UnloadedStart.extentState(level, pos, original.call(level, pos), firstEnd, start, startState);
    }

    @WrapOperation(
        method = "tryConnect",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/trains/track/TrackPlacement;placeTracks(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/trains/track/TrackPlacement$PlacementInfo;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Z)Lcom/simibubi/create/content/trains/track/TrackPlacement$PlacementInfo;"
        )
    )
    private static TrackPlacement.PlacementInfo trackwright$boreAndConvert(
        Level level,
        TrackPlacement.PlacementInfo info,
        BlockState state1,
        BlockState state2,
        BlockPos targetPos1,
        BlockPos targetPos2,
        boolean simulate,
        Operation<TrackPlacement.PlacementInfo> original,
        @Local(argsOnly = true) Player player,
        @Local(argsOnly = true) BlockPos pos2,
        @Local(name = "pos1") BlockPos pos1
    ) {
        return PlacementRules.placeTracks(level, player, info, pos1, pos2, simulate,
            () -> original.call(level, info, state1, state2, targetPos1, targetPos2, simulate));
    }

    @WrapOperation(
        method = "placeTracks",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z")
    )
    private static boolean trackwright$recordTrackForUndo(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        UndoRecorder.beforeWrite(level, pos);
        return original.call(level, pos, state, flags);
    }

    @WrapOperation(
        method = "tryConnect",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;")
    )
    private static ItemStack trackwright$recordChargeForUndo(
        ItemStack slot,
        int remaining,
        Operation<ItemStack> original,
        @Local(argsOnly = true) Player player
    ) {
        UndoRecorder.charged(player, slot, remaining);
        return original.call(slot, remaining);
    }

    @Inject(
        method = "tryConnect",
        at = @At(
            value = "FIELD",
            target = "Lcom/simibubi/create/content/trains/track/TrackPlacement$PlacementInfo;valid:Z",
            opcode = Opcodes.PUTFIELD,
            ordinal = 0,
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private static void trackwright$refuseOverlaps(
        CallbackInfoReturnable<TrackPlacement.PlacementInfo> cir,
        @Local(argsOnly = true) Player player,
        @Local(name = "level") Level level,
        @Local(name = "info") TrackPlacement.PlacementInfo info,
        @Local(name = "pos1") BlockPos pos1,
        @Local(name = "pos2") BlockPos pos2,
        @Local(name = "axis1") Vec3 axis1,
        @Local(name = "axis2") Vec3 axis2
    ) {
        String refusal = PlacementRules.refusal(level, player, info, pos1, pos2, axis1, axis2);
        if (refusal != null) {
            info.valid = false;
            cir.setReturnValue(info.withMessage(refusal).tooJumbly());
        }
    }
}
