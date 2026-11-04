package dev.chaevsfe.trackwright.client.controls;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public final class TrainControlGesture {
    private TrainControlGesture() {
    }

    public static boolean opensScreen(AbstractContraptionEntity entity, Player player, BlockPos localPos, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || AllItems.WRENCH.isIn(player.getMainHandItem())) {
            return false;
        }
        if (!(entity instanceof CarriageContraptionEntity carriage) || !entity.level().isClientSide()) {
            return false;
        }
        ControlTarget target = TrainControlKeys.targetAt(carriage, localPos);
        if (target == null) {
            return false;
        }
        TrainControlKeys.openAssign(Minecraft.getInstance(), target);
        return true;
    }
}
