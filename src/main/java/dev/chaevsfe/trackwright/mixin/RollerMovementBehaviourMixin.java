package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.content.contraptions.actors.roller.RollerBlockEntity;
import com.zurrtum.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.chaevsfe.trackwright.actors.RollerRules;
import dev.chaevsfe.trackwright.actors.TrainActors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RollerMovementBehaviour.class)
public abstract class RollerMovementBehaviourMixin {
    @Unique
    private boolean trackwright$skipColumn;

    @Shadow
    protected abstract BlockState getStateToPaveWith(MovementContext context);

    @Shadow
    protected abstract BlockState getStateToPaveWithAsSlab(MovementContext context);

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
        return TrainActors.canBreak(behaviour, context, level, pos, state, original.call(behaviour, level, pos, state))
            && RollerRules.reshapeMayBreak(context, state, getStateToPaveWith(context), getStateToPaveWithAsSlab(context));
    }

    @Inject(method = "triggerPaver", at = @At("HEAD"), cancellable = true)
    private void trackwright$noAirPaving(MovementContext context, BlockPos pos, CallbackInfo ci) {
        if (TrainActors.skipsTunnelPaving(context) && getStateToPaveWith(context).isAir()) {
            ci.cancel();
        }
    }

    @Inject(method = "triggerPaver", at = @At("HEAD"))
    private void trackwright$forgetColumn(MovementContext context, BlockPos pos, CallbackInfo ci) {
        trackwright$skipColumn = false;
    }

    @Inject(
        method = "triggerPaver",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/contraptions/actors/roller/RollerMovementBehaviour;tryFill(Lcom/zurrtum/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lcom/zurrtum/create/content/contraptions/actors/roller/RollerMovementBehaviour$PaveResult;"),
        require = 2,
        allow = 2
    )
    private void trackwright$needsGround(
        MovementContext context,
        BlockPos pos,
        CallbackInfo ci,
        @Local(name = "currentPos") Pair<BlockPos, Boolean> currentPos,
        @Local(name = "yOffset") int depth,
        @Local(name = "mode") RollerBlockEntity.RollingMode mode,
        @Local(name = "stateToPaveWith") BlockState paveWith
    ) {
        trackwright$skipColumn = RollerRules.skipsForGround(context, mode, currentPos.getFirst(), depth, paveWith);
    }

    @Inject(method = "triggerPaver", at = @At("RETURN"))
    private void trackwright$columnDone(MovementContext context, BlockPos pos, CallbackInfo ci) {
        trackwright$skipColumn = false;
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
        return !trackwright$skipColumn && original.call(level, pos) && !TrainActors.leavesFluid(context, level, pos, toPlace)
            && !RollerRules.reshapeRefusesPaving(context, level, pos, toPlace);
    }
}
