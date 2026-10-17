package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.AllHandle;
import com.zurrtum.create.infrastructure.packet.c2s.CurvedTrackDestroyPacket;
import com.zurrtum.create.infrastructure.packet.c2s.CurvedTrackSelectionPacket;
import com.zurrtum.create.infrastructure.packet.c2s.ScheduleEditPacket;
import dev.chaevsfe.trackwright.schedule.ActorSteps;
import dev.chaevsfe.trackwright.track.ConnectionLength;
import net.minecraft.core.BlockPos;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(AllHandle.class)
public abstract class AllHandleMixin {
    @WrapOperation(
        method = "onCurvedTrackDestroy",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/AllHandle;onBlockEntityConfiguration(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;Lnet/minecraft/core/BlockPos;ILjava/util/function/Predicate;)V")
    )
    private static void trackwright$reachAlongDestroyedCurve(
        ServerGamePacketListenerImpl listener,
        BlockPos pos,
        int distance,
        Predicate<BlockEntity> predicate,
        Operation<Void> original,
        @Local(argsOnly = true) CurvedTrackDestroyPacket packet
    ) {
        trackwright$withCurveReach(listener, pos, packet.targetPos(), distance, predicate, original);
    }

    @WrapOperation(
        method = "onCurvedTrackSelection",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/AllHandle;onBlockEntityConfiguration(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;Lnet/minecraft/core/BlockPos;ILjava/util/function/Predicate;)V")
    )
    private static void trackwright$reachAlongSelectedCurve(
        ServerGamePacketListenerImpl listener,
        BlockPos pos,
        int distance,
        Predicate<BlockEntity> predicate,
        Operation<Void> original,
        @Local(argsOnly = true) CurvedTrackSelectionPacket packet
    ) {
        trackwright$withCurveReach(listener, pos, packet.targetPos(), distance, predicate, original);
    }

    @WrapMethod(method = "onScheduleEdit")
    private static void trackwright$keepActorSteps(ServerGamePacketListenerImpl listener, ScheduleEditPacket packet, Operation<Void> original) {
        if (!ActorSteps.refuseEdit(listener.player)) {
            original.call(listener, packet);
        }
    }

    @Unique
    private static void trackwright$withCurveReach(
        ServerGamePacketListenerImpl listener,
        BlockPos pos,
        BlockPos target,
        int distance,
        Predicate<BlockEntity> predicate,
        Operation<Void> original
    ) {
        int reach = ConnectionLength.curveLength(listener.player.level(), pos, target);
        if (reach + ConnectionLength.CURVE_REACH_MARGIN <= distance) {
            original.call(listener, pos, distance, predicate);
            return;
        }
        Integer previous = ConnectionLength.enterCurveReach(reach);
        try {
            original.call(listener, pos, reach + ConnectionLength.CURVE_REACH_MARGIN, predicate);
        } finally {
            ConnectionLength.exitCurveReach(previous);
        }
    }
}
