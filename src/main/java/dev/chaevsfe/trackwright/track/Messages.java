package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
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
    public static final String GAP = "trackwright.gap";
    public static final String GAP_AT = "trackwright.gap_at";
    public static final String GAP_AT_BORE = "trackwright.gap_at_bore";
    public static final String GAP_BLOCK = "trackwright.gap_block";
    public static final String GAP_START = "trackwright.gap_start";
    public static final String GAP_START_AT = "trackwright.gap_start_at";
    public static final String GAP_START_AT_BORE = "trackwright.gap_start_at_bore";
    public static final String GAP_START_BLOCK = "trackwright.gap_start_block";

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
        Map.entry(KEY_PREFIX + CONVERT_TRAIN, "A train is on this straight"),
        Map.entry(KEY_PREFIX + GAP, "A block in the way would leave a gap in the track"),
        Map.entry(KEY_PREFIX + GAP_AT, "%s at %s is in the way and would leave a gap in the track"),
        Map.entry(KEY_PREFIX + GAP_AT_BORE, "%s at %s is in the way and would leave a gap in the track; hold a pickaxe in your off hand to mine through"),
        Map.entry(KEY_PREFIX + GAP_BLOCK, "%s is in the way and would leave a gap in the track"),
        Map.entry(KEY_PREFIX + GAP_START, "A block where the curve starts would be destroyed by the track"),
        Map.entry(KEY_PREFIX + GAP_START_AT, "%s at %s would be destroyed where the curve starts"),
        Map.entry(KEY_PREFIX + GAP_START_AT_BORE, "%s at %s would be destroyed where the curve starts; hold a pickaxe in your off hand to mine it"),
        Map.entry(KEY_PREFIX + GAP_START_BLOCK, "%s would be destroyed where the curve starts")
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

    public static boolean isOwnOverlay(Component component) {
        return component.getContents() instanceof TranslatableContents translatable && FALLBACKS.containsKey(translatable.getKey());
    }

    public static @Nullable MutableComponent named(String key, PlacementInfo info) {
        Gap gap = gapFor(key, info);
        if (gap == null) {
            return null;
        }
        return positioned(gap, ((PlacementInfoExtension) info).trackwright$boreAccess() == BoreAccess.NO_TOOL);
    }

    public static Component overlayFor(Player player, Component message, PlacementInfo info) {
        if (info.message != null && player instanceof ServerPlayer serverPlayer && TrackwrightNetwork.hasTrackwright(serverPlayer)) {
            Gap gap = gapFor("create." + info.message, info);
            if (gap != null) {
                if (!seenBy(serverPlayer, gap.pos())) {
                    return positioned(gap, false);
                }
                String brief = KEY_PREFIX + (gap.start() ? GAP_START_BLOCK : GAP_BLOCK);
                return Component.translatableWithFallback(brief, FALLBACKS.get(brief), gap.state().getBlock().getName());
            }
        }
        return message;
    }

    private static MutableComponent positioned(Gap gap, boolean hint) {
        String named = KEY_PREFIX + (gap.start() ? hint ? GAP_START_AT_BORE : GAP_START_AT : hint ? GAP_AT_BORE : GAP_AT);
        BlockPos pos = gap.pos();
        return Component.translatableWithFallback(named, FALLBACKS.get(named), gap.state().getBlock().getName(),
            pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }

    private static boolean seenBy(ServerPlayer player, BlockPos pos) {
        return player.getChunkTrackingView().contains(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))
            && !player.connection.chunkSender.isPending(ChunkPos.pack(pos));
    }

    private static @Nullable Gap gapFor(String key, PlacementInfo info) {
        if (!(KEY_PREFIX + GAP).equals(key) && !(KEY_PREFIX + GAP_START).equals(key)) {
            return null;
        }
        return ((PlacementInfoExtension) info).trackwright$gap();
    }

    public static MutableComponent overlay(String message) {
        String key = KEY_PREFIX + message;
        String fallback = FALLBACKS.get(key);
        return fallback != null ? Component.translatableWithFallback(key, fallback) : Component.translatable(key);
    }
}
