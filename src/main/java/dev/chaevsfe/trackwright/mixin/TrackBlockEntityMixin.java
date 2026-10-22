package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import dev.chaevsfe.trackwright.track.LoadedColumns;
import dev.chaevsfe.trackwright.undo.UndoRecorder;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(TrackBlockEntity.class)
public abstract class TrackBlockEntityMixin {
    @WrapOperation(
        method = "manageFakeTracksAlong",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/track/BezierConnection;rasterise()Ljava/util/Map;")
    )
    private Map<Pair<Integer, Integer>, Double> trackwright$loadedColumnsOnly(
        BezierConnection connection,
        Operation<Map<Pair<Integer, Integer>, Double>> original
    ) {
        return LoadedColumns.filter(((BlockEntity) (Object) this).getLevel(), connection.bePositions.getFirst(), original.call(connection));
    }

    @WrapOperation(
        method = "manageFakeTracksAlong",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z")
    )
    private boolean trackwright$recordFakeTrackForUndo(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        UndoRecorder.beforeWrite(level, pos);
        return original.call(level, pos, state, flags);
    }
}
