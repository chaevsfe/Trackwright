package dev.chaevsfe.trackwright.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.infrastructure.packet.c2s.PlaceExtendedCurvePacket;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class WidestTurnLock {
    public static final String KEY_NAME = "trackwright.key.lock_widest_turn";

    private static @Nullable KeyMapping key;
    private static boolean locked;

    private WidestTurnLock() {
    }

    public static void register(KeyMapping.Category category) {
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY_NAME, InputConstants.UNKNOWN.getValue(), category));
    }

    public static void tick(Minecraft mc) {
        KeyMapping mapping = key;
        if (mapping == null) {
            return;
        }
        while (mapping.consumeClick()) {
            locked = !locked;
            LocalPlayer player = mc.player;
            if (player != null) {
                player.sendOverlayMessage(Component.translatable(locked ? "trackwright.lock.on" : "trackwright.lock.off"));
                ActionBarHold.start();
            }
        }
    }

    public static boolean locked() {
        return locked;
    }

    public static boolean bound() {
        KeyMapping mapping = key;
        return mapping != null && !mapping.isUnbound();
    }

    public static void beforeUse(UseOnContext context) {
        if (wanted(context.getLevel(), context.getHand(), context.getPlayer(), context.getItemInHand())
            && !onTrackBlock(context.getLevel(), context.getClickedPos())) {
            send();
        }
    }

    public static InteractionResult beforeBlockUse(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (wanted(level, hand, player, player.getItemInHand(hand)) && onTrackBlock(level, hit.getBlockPos())) {
            send();
        }
        return InteractionResult.PASS;
    }

    private static boolean wanted(Level level, InteractionHand hand, @Nullable Player player, ItemStack stack) {
        return locked && level.isClientSide() && hand == InteractionHand.MAIN_HAND
            && player instanceof LocalPlayer local && !local.isShiftKeyDown()
            && stack.has(AllDataComponents.TRACK_CONNECTING_FROM)
            && !Minecraft.getInstance().options.keySprint.isDown();
    }

    private static boolean onTrackBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ITrackBlock;
    }

    private static void send() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new PlaceExtendedCurvePacket(true, true));
        }
    }
}
