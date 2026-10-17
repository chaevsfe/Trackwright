package dev.chaevsfe.trackwright.undo;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UndoMessages {
    public static final String DONE = "trackwright.undo.done";
    public static final String RETURNED = "trackwright.undo.returned";
    public static final String ITEMS = "trackwright.undo.items";
    public static final String NOTHING = "trackwright.undo.nothing";
    public static final String OTHER_DIMENSION = "trackwright.undo.other_dimension";
    public static final String NOT_LOADED = "trackwright.undo.not_loaded";
    public static final String TOO_FAR = "trackwright.undo.too_far";
    public static final String NOT_ALLOWED = "trackwright.undo.not_allowed";
    public static final String PROTECTED = "trackwright.undo.protected";
    public static final String CHANGED = "trackwright.undo.changed";
    public static final String JOINED = "trackwright.undo.joined";
    public static final String TRAIN = "trackwright.undo.train";
    public static final String POINTS = "trackwright.undo.points";
    public static final String IN_THE_WAY = "trackwright.undo.in_the_way";
    public static final String DROPS_GONE = "trackwright.undo.drops_gone";
    public static final String EXPERIENCE = "trackwright.undo.experience";
    public static final String CONVERTED = "trackwright.undo.converted";
    public static final String PLAYERS_ONLY = "trackwright.undo.players_only";

    private static final Map<String, String> FALLBACKS = Map.ofEntries(
        Map.entry(DONE, "Undid your last track placement"),
        Map.entry(RETURNED, "Undid your last track placement and returned %s"),
        Map.entry(ITEMS, "%s %s"),
        Map.entry(NOTHING, "Nothing to undo: no track laid by you in the last 2 minutes"),
        Map.entry(OTHER_DIMENSION, "Your last track placement is in another dimension"),
        Map.entry(NOT_LOADED, "Part of your last track placement is not loaded"),
        Map.entry(TOO_FAR, "Walk closer to your last track placement to undo it"),
        Map.entry(NOT_ALLOWED, "You cannot change blocks here"),
        Map.entry(PROTECTED, "Part of your last track placement is protected"),
        Map.entry(CHANGED, "Your last track placement has changed since you laid it"),
        Map.entry(JOINED, "Another track now joins your last track placement"),
        Map.entry(TRAIN, "A train is on your last track placement"),
        Map.entry(POINTS, "Remove the stations, signals and observers on your last track placement first"),
        Map.entry(IN_THE_WAY, "Something is standing where blocks would be put back"),
        Map.entry(DROPS_GONE, "The blocks mined for your last track placement are no longer in your inventory"),
        Map.entry(EXPERIENCE, "Mining for your last track placement gave experience, so it cannot be undone"),
        Map.entry(CONVERTED, "Track laid over a straight through blocks cannot be undone"),
        Map.entry(PLAYERS_ONLY, "Only a player can undo a track placement")
    );

    private UndoMessages() {
    }

    public static MutableComponent text(String key, Object... arguments) {
        return Component.translatableWithFallback(key, FALLBACKS.get(key), arguments);
    }

    static MutableComponent refusal(String key) {
        return text(key).withStyle(ChatFormatting.RED);
    }

    static MutableComponent done(List<ItemStack> returned) {
        List<ItemStack> kinds = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (ItemStack stack : returned) {
            int index = -1;
            for (int i = 0; i < kinds.size(); i++) {
                if (ItemStack.isSameItemSameComponents(kinds.get(i), stack)) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                kinds.add(stack);
                counts.add(stack.getCount());
            } else {
                counts.set(index, counts.get(index) + stack.getCount());
            }
        }
        if (kinds.isEmpty()) {
            return text(DONE).withStyle(ChatFormatting.GREEN);
        }
        MutableComponent items = Component.empty();
        for (int i = 0; i < kinds.size(); i++) {
            if (i > 0) {
                items.append(Component.literal(", "));
            }
            items.append(text(ITEMS, Component.literal(Integer.toString(counts.get(i))), kinds.get(i).getHoverName()));
        }
        return text(RETURNED, items).withStyle(ChatFormatting.GREEN);
    }
}
