package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.chaevsfe.trackwright.actors.TrainActors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RollerMovementBehaviour.class)
public abstract class RollerMovementBehaviourMixin {
    @Shadow
    protected abstract BlockState getStateToPaveWith(MovementContext context);

    @WrapOperation(
        method = "testBreakerTarget",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/contraptions/actors/roller/RollerMovementBehaviour;canBreak(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z")
    )
    private boolean trackwright$trainRollerBreaks(
        RollerMovementBehaviour behaviour,
        Level level,
        BlockPos pos,
        BlockState state,
        Operation<Boolean> original,
        @Local(argsOnly = true) MovementContext context
    ) {
        return TrainActors.canBreak(behaviour, context, level, pos, state, original.call(behaviour, level, pos, state));
    }

    @Inject(method = "triggerPaver", at = @At("HEAD"), cancellable = true)
    private void trackwright$noAirPaving(MovementContext context, BlockPos pos, CallbackInfo ci) {
        if (TrainActors.skipsTunnelPaving(context) && getStateToPaveWith(context).isAir()) {
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "tryFill",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isLoaded(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean trackwright$leaveFluids(
        Level level,
        BlockPos pos,
        Operation<Boolean> original,
        @Local(argsOnly = true) MovementContext context,
        @Local(argsOnly = true) BlockState toPlace
    ) {
        return original.call(level, pos) && !TrainActors.leavesFluid(context, level, pos, toPlace);
    }
}
