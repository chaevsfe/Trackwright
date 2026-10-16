package dev.chaevsfe.trackwright.track;

import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public final class Messages {
    public static final String OVERLAPS_STRAIGHT = "trackwright.overlaps_straight";

    private static final String KEY_PREFIX = "create.track.";
    private static final Map<String, String> FALLBACKS = Map.of(
        KEY_PREFIX + OVERLAPS_STRAIGHT, "This would overlap a straight track connection"
    );

    private Messages() {
    }

    public static @Nullable String fallback(String key) {
        return FALLBACKS.get(key);
    }

    public static boolean isOwn(@Nullable String message) {
        return message != null && FALLBACKS.containsKey("create." + message);
    }

    public static void repeatInChatWithoutTrackwright(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer && !TrackwrightNetwork.hasTrackwright(serverPlayer)) {
            serverPlayer.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED));
        }
    }

    public static MutableComponent overlay(String message) {
        String key = KEY_PREFIX + message;
        String fallback = FALLBACKS.get(key);
        return fallback != null ? Component.translatableWithFallback(key, fallback) : Component.translatable(key);
    }
}
