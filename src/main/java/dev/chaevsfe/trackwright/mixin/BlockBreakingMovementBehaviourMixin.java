package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import dev.chaevsfe.trackwright.actors.TrainActors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBreakingMovementBehaviour.class)
public abstract class BlockBreakingMovementBehaviourMixin {
    @WrapOperation(
        method = {"visitNewPosition", "tickBreaker"},
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/kinetics/base/BlockBreakingMovementBehaviour;canBreak(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
        require = 2,
        allow = 2
    )
    private boolean trackwright$trainBreaks(
        BlockBreakingMovementBehaviour behaviour,
        Level level,
        BlockPos pos,
        BlockState state,
        Operation<Boolean> original,
        @Local(argsOnly = true) MovementContext context
    ) {
        return TrainActors.canBreak(behaviour, context, level, pos, state, original.call(behaviour, level, pos, state));
    }
}
