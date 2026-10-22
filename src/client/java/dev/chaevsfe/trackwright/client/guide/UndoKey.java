package dev.chaevsfe.trackwright.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllTags.AllItemTags;
import dev.chaevsfe.trackwright.client.TrackwrightClient;
import dev.chaevsfe.trackwright.network.UndoPayload;
import dev.chaevsfe.trackwright.network.UndoStatePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

public final class UndoKey {
    public static final String KEY_NAME = "trackwright.key.undo";
    private static final long MILLIS_PER_TICK = 50;

    private static @Nullable KeyMapping key;
    private static int placements;
    private static long until;

    private UndoKey() {
    }

    public static KeyMapping create() {
        KeyMapping mapping = new KeyMapping(KEY_NAME, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_U, TrackwrightClient.KEY_CATEGORY);
        key = mapping;
        return mapping;
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
            ClientPacketListener connection = mc.getConnection();
            if (connection != null && connection.hasChannel(UndoPayload.TYPE)) {
                PacketDistributor.sendToServer(UndoPayload.request());
            } else {
                player.displayClientMessage(Component.translatable("trackwright.undo.unsupported").withStyle(ChatFormatting.RED), true);
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
        return player.getMainHandItem().is(AllItemTags.TRACKS.tag) || player.getOffhandItem().is(AllItemTags.TRACKS.tag);
    }
}
