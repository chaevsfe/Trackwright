package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueSettingsInputHandler;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.BehaviourType;
import dev.chaevsfe.trackwright.client.actors.RollerTerrainScroll;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ValueSettingsInputHandler.class)
public abstract class ValueSettingsInputHandlerMixin {
    @WrapOperation(
        method = "onBlockActivated",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/client/foundation/blockEntity/behaviour/ValueSettingsInputHandler;handleInteraction(Lcom/zurrtum/create/api/behaviour/BlockEntityBehaviour;Lcom/zurrtum/create/foundation/blockEntity/behaviour/BehaviourType;Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/BlockPos;Lcom/zurrtum/create/foundation/blockEntity/SmartBlockEntity;)Lnet/minecraft/world/InteractionResult;")
    )
    private static InteractionResult trackwright$rollerTerrainBox(
        BlockEntityBehaviour<?> behaviour,
        BehaviourType<? extends BlockEntityBehaviour<?>> type,
        LocalPlayer player,
        InteractionHand hand,
        BlockHitResult hit,
        ItemStack held,
        BlockPos pos,
        SmartBlockEntity blockEntity,
        Operation<InteractionResult> original
    ) {
        InteractionResult result = original.call(behaviour, type, player, hand, hit, held, pos, blockEntity);
        if (result != null || type != ScrollValueBehaviour.TYPE) {
            return result;
        }
        RollerTerrainScroll terrain = blockEntity.getBehaviour(RollerTerrainScroll.TYPE);
        if (terrain == null) {
            return null;
        }
        return original.call(terrain, RollerTerrainScroll.TYPE, player, hand, hit, held, pos, blockEntity);
    }
}
