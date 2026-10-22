package dev.chaevsfe.trackwright.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.PlaceExtendedCurvePacket;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
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
        if (!locked || !context.getLevel().isClientSide() || context.getHand() != InteractionHand.MAIN_HAND
            || !(context.getPlayer() instanceof LocalPlayer player) || player.isShiftKeyDown()
            || !context.getItemInHand().has(AllDataComponents.TRACK_CONNECTING_FROM)
            || Minecraft.getInstance().options.keySprint.isDown()) {
            return;
        }
        player.connection.send(new PlaceExtendedCurvePacket(true, true));
    }
}
