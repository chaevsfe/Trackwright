package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import dev.chaevsfe.trackwright.actors.DrillFace;
import dev.chaevsfe.trackwright.actors.TrainActors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBreakingMovementBehaviour.class)
public abstract class BlockBreakingMovementBehaviourMixin {
    @Inject(method = "visitNewPosition", at = @At("RETURN"))
    private void trackwright$queueDrillFace(MovementContext context, BlockPos pos, CallbackInfo ci) {
        DrillFace.afterVisit((BlockBreakingMovementBehaviour) (Object) this, context, pos);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void trackwright$breakDrillFace(MovementContext context, CallbackInfo ci) {
        DrillFace.afterTick((BlockBreakingMovementBehaviour) (Object) this, context);
    }

    @Inject(method = "cancelStall", at = @At("HEAD"))
    private void trackwright$dropDrillFace(MovementContext context, CallbackInfo ci) {
        DrillFace.reset(context.data);
    }

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
