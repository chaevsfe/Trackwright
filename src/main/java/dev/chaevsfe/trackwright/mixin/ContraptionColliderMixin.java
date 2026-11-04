package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.contraptions.ContraptionCollider;
import com.zurrtum.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import dev.chaevsfe.trackwright.markers.Markers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ContraptionCollider.class)
public abstract class ContraptionColliderMixin {
    @WrapOperation(
        method = "isCollidingWithWorld(Lnet/minecraft/world/level/Level;Lcom/zurrtum/create/content/contraptions/TranslatingContraption;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/kinetics/base/BlockBreakingMovementBehaviour;canBreak(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
        require = 1,
        allow = 1
    )
    private static boolean trackwright$markedBlocksStopDrills(
        BlockBreakingMovementBehaviour behaviour,
        Level level,
        BlockPos pos,
        BlockState state,
        Operation<Boolean> original
    ) {
        return original.call(behaviour, level, pos, state) && !Markers.isProtected(level, pos);
    }
}
