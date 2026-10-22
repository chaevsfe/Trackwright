package dev.chaevsfe.trackwright.track;

import com.simibubi.create.AllDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class WidestTurnFlag {
    private static final ThreadLocal<Boolean> TRACK_USED = ThreadLocal.withInitial(() -> false);

    private WidestTurnFlag() {
    }

    public static void beforeUseOnBlock() {
        TRACK_USED.set(false);
    }

    public static void trackUsed(Level level) {
        if (!level.isClientSide()) {
            TRACK_USED.set(true);
        }
    }

    public static void afterUseOnBlock(ServerPlayer player, InteractionHand hand) {
        boolean used = TRACK_USED.get();
        TRACK_USED.set(false);
        if (used) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.has(AllDataComponents.TRACK_EXTENDED_CURVE)) {
            stack.remove(AllDataComponents.TRACK_EXTENDED_CURVE);
        }
    }
}
