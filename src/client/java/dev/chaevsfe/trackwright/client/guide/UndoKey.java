package dev.chaevsfe.trackwright.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllTags.AllItemTags;
import dev.chaevsfe.trackwright.network.UndoPayload;
import dev.chaevsfe.trackwright.network.UndoStatePayload;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public final class UndoKey {
    public static final String KEY_NAME = "trackwright.key.undo";
    private static final long MILLIS_PER_TICK = 50;

    private static @Nullable KeyMapping key;
    private static int placements;
    private static long until;

    private UndoKey() {
    }

    public static void register(KeyMapping.Category category) {
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY_NAME, InputConstants.KEY_U, category));
    }

    public static void tick(Minecraft mc) {
        KeyMapping mapping = key;
        if (mapping == null) {
            return;
        }
        while (mapping.consumeClick()) {
            LocalPlayer player = mc.player;
            if (player == null || !holdsTrack(player)) {
                continue;
            }
            if (ClientPlayNetworking.canSend(UndoPayload.TYPE)) {
                ClientPlayNetworking.send(UndoPayload.request());
            } else {
                player.sendOverlayMessage(Component.translatable("trackwright.undo.unsupported").withStyle(ChatFormatting.RED));
                ActionBarHold.start();
            }
        }
    }

    public static void receive(UndoStatePayload payload) {
        placements = payload.placements();
        until = Util.getMillis() + payload.ticksLeft() * MILLIS_PER_TICK;
        if (payload.result()) {
            ActionBarHold.start();
        }
    }

    public static void reset() {
        placements = 0;
        until = 0;
    }

    public static boolean live() {
        return placements > 0 && Util.getMillis() < until;
    }

    public static boolean bound() {
        KeyMapping mapping = key;
        return mapping != null && !mapping.isUnbound();
    }

    private static boolean holdsTrack(LocalPlayer player) {
        return player.getMainHandItem().is(AllItemTags.TRACKS) || player.getOffhandItem().is(AllItemTags.TRACKS);
    }
}
