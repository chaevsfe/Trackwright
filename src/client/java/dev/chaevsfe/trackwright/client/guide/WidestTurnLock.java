package dev.chaevsfe.trackwright.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.PlaceExtendedCurvePacket;
import dev.chaevsfe.trackwright.client.TrackwrightClient;
import net.createmod.catnip.platform.CatnipServices;
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

    public static KeyMapping create() {
        KeyMapping mapping = new KeyMapping(KEY_NAME, InputConstants.UNKNOWN.getValue(), TrackwrightClient.KEY_CATEGORY);
        key = mapping;
        return mapping;
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
                player.displayClientMessage(Component.translatable(locked ? "trackwright.lock.on" : "trackwright.lock.off"), true);
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
        CatnipServices.NETWORK.sendToServer(new PlaceExtendedCurvePacket(true, true));
    }
}
