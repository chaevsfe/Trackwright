package dev.chaevsfe.trackwright.markers;

import dev.chaevsfe.trackwright.config.SettingMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;

public final class MarkerMessages {
    public static final String MARKED = "trackwright.marker.marked";
    public static final String UNMARKED = "trackwright.marker.unmarked";
    public static final String PROTECTED = "trackwright.marker.protected";
    public static final String SYMMETRY = "trackwright.marker.symmetry";
    public static final String BOX_TOO_BIG = "trackwright.marker.box_too_big";
    public static final String COUNT_TOO_BIG = "trackwright.marker.count_too_big";
    public static final String DONE_MARK = "trackwright.marker.done_mark";
    public static final String DONE_UNMARK = "trackwright.marker.done_unmark";
    public static final String COUNT = "trackwright.marker.count";
    public static final String COUNT_OFF = "trackwright.marker.count_off";
    public static final String TOOLTIP = "trackwright.marker.tooltip";

    private static final Map<String, String> FALLBACKS = Map.ofEntries(
        Map.entry(MARKED, "Marked: contraptions will not break this block"),
        Map.entry(UNMARKED, "Unmarked: contraptions may break this block again"),
        Map.entry(PROTECTED, "You may not mark blocks here"),
        Map.entry(SYMMETRY, "Put the Wand of Symmetry out of your hotbar first"),
        Map.entry(BOX_TOO_BIG, "That box holds %1$s blocks; the most is %2$s"),
        Map.entry(COUNT_TOO_BIG, "That box spans %1$s chunks; the most is %2$s"),
        Map.entry(DONE_MARK, "Marked %1$s blocks (%2$s skipped: protected or not loaded)"),
        Map.entry(DONE_UNMARK, "Unmarked %1$s blocks (%2$s skipped: protected or not loaded)"),
        Map.entry(COUNT, "%1$s marked blocks in that box"),
        Map.entry(COUNT_OFF, "%1$s marked blocks in that box; markers are off on this server, so contraptions ignore them"),
        Map.entry(TOOLTIP, "Sneak + right-click: mark a block contraptions will not break")
    );

    private MarkerMessages() {
    }

    public static MutableComponent text(String key, Object... args) {
        String fallback = FALLBACKS.get(key);
        if (fallback == null) {
            return args.length == 0 ? SettingMessages.component(key) : Component.translatable(key, args);
        }
        return Component.translatableWithFallback(key, fallback, args);
    }

    public static MutableComponent refusal(String key, Object... args) {
        return text(key, args).withStyle(ChatFormatting.RED);
    }
}
