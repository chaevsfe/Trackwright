package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.TrackPaver;
import dev.chaevsfe.trackwright.undo.UndoRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TrackPaver.class)
public abstract class TrackPaverMixin {
    @WrapOperation(
        method = "placeBlockIfFree",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z")
    )
    private static boolean trackwright$recordPavingForUndo(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        UndoRecorder.beforeWrite(level, pos);
        return original.call(level, pos, state, flags);
    }
}
