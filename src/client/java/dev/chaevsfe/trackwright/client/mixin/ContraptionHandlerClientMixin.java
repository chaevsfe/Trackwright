package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import dev.chaevsfe.trackwright.client.controls.TrainControlGesture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ContraptionHandlerClient.class)
public abstract class ContraptionHandlerClientMixin {
    @WrapOperation(
        method = "rightClickingOnContraptionsGetsHandledLocally(Lnet/neoforged/neoforge/client/event/InputEvent$InteractionKeyMappingTriggered;)V",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;handlePlayerInteraction(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/InteractionHand;)Z"),
        require = 1,
        allow = 1
    )
    private static boolean trackwright$assignTrainControl(
        AbstractContraptionEntity entity,
        Player player,
        BlockPos localPos,
        Direction side,
        InteractionHand hand,
        Operation<Boolean> original
    ) {
        if (TrainControlGesture.opensScreen(entity, player, localPos, hand)) {
            return false;
        }
        return original.call(entity, player, localPos, side, hand);
    }
}
