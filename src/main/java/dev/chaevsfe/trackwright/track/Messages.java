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
    public static final String BORE_NOT_LOADED = "trackwright.bore_not_loaded";
    public static final String BORE_UNBREAKABLE = "trackwright.bore_unbreakable";
    public static final String BORE_BLOCK_ENTITY = "trackwright.bore_block_entity";
    public static final String BORE_IMMUNE = "trackwright.bore_immune";
    public static final String BORE_TOO_HARD = "trackwright.bore_too_hard";
    public static final String BORE_WRONG_TOOL = "trackwright.bore_wrong_tool";
    public static final String BORE_TOOL_WORN = "trackwright.bore_tool_worn";
    public static final String BORE_PROTECTED = "trackwright.bore_protected";
    public static final String BORE_LAVA = "trackwright.bore_lava";
    public static final String BORE_FALLING = "trackwright.bore_falling";
    public static final String BORE_STOPPED = "trackwright.bore_stopped";
    public static final String CONVERT_POINTS = "trackwright.convert_points";
    public static final String CONVERT_TRAIN = "trackwright.convert_train";

    private static final String KEY_PREFIX = "create.track.";
    private static final Map<String, String> FALLBACKS = Map.ofEntries(
        Map.entry(KEY_PREFIX + OVERLAPS_STRAIGHT, "This would overlap a straight track connection"),
        Map.entry(KEY_PREFIX + BORE_NOT_LOADED, "Part of the line is not loaded"),
        Map.entry(KEY_PREFIX + BORE_UNBREAKABLE, "An unbreakable block is in the way"),
        Map.entry(KEY_PREFIX + BORE_BLOCK_ENTITY, "A container or machine is in the way"),
        Map.entry(KEY_PREFIX + BORE_IMMUNE, "A block in the way may not be mined"),
        Map.entry(KEY_PREFIX + BORE_TOO_HARD, "A block in the way is too hard to mine on this server"),
        Map.entry(KEY_PREFIX + BORE_WRONG_TOOL, "The off-hand tool cannot mine every block in the way"),
        Map.entry(KEY_PREFIX + BORE_TOOL_WORN, "The off-hand tool would break before the end"),
        Map.entry(KEY_PREFIX + BORE_PROTECTED, "Part of the line is protected"),
        Map.entry(KEY_PREFIX + BORE_LAVA, "Mining this would let lava in"),
        Map.entry(KEY_PREFIX + BORE_FALLING, "Sand or gravel above the line would fall in"),
        Map.entry(KEY_PREFIX + BORE_STOPPED, "Mining stopped at a protected block, so no track was laid"),
        Map.entry(KEY_PREFIX + CONVERT_POINTS, "Remove the stations, signals and observers on this straight first"),
        Map.entry(KEY_PREFIX + CONVERT_TRAIN, "A train is on this straight")
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
