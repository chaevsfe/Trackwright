package dev.chaevsfe.trackwright.track;

import dev.chaevsfe.trackwright.mining.BlockMiner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;

public final class ClearMessages {
    public static final String CONFIRM = "trackwright.clear_confirm";
    public static final String CONFIRM_ONE = "trackwright.clear_confirm_one";
    public static final String CHANGED = "trackwright.clear_changed";
    public static final String CHANGED_ONE = "trackwright.clear_changed_one";
    public static final String DONE = "trackwright.clear_done";
    public static final String DONE_ONE = "trackwright.clear_done_one";
    public static final String STOPPED = "trackwright.clear_stopped";
    public static final String STOPPED_ONE = "trackwright.clear_stopped_one";
    public static final String WRONG_TOOL = "trackwright.clear_wrong_tool";
    public static final String TOOL_WORN = "trackwright.clear_tool_worn";
    public static final String TOO_FAR = "trackwright.clear_too_far";
    public static final String NO_CONNECTION = "trackwright.clear_no_connection";
    public static final String NOTHING = "trackwright.clear_nothing";

    private static final Map<String, String> FALLBACKS = Map.ofEntries(
        Map.entry(CONFIRM, "Clear %s blocks from this track bed? Sneak and right-click it again within 10 seconds"),
        Map.entry(CONFIRM_ONE, "Clear %s block from this track bed? Sneak and right-click it again within 10 seconds"),
        Map.entry(CHANGED, "The track bed changed. Right-click again to clear %s blocks"),
        Map.entry(CHANGED_ONE, "The track bed changed. Right-click again to clear %s block"),
        Map.entry(DONE, "Cleared %s blocks"),
        Map.entry(DONE_ONE, "Cleared %s block"),
        Map.entry(STOPPED, "Clearing stopped at a protected block after %s blocks"),
        Map.entry(STOPPED_ONE, "Clearing stopped at a protected block after %s block"),
        Map.entry(WRONG_TOOL, "Your tool cannot mine every block in the way"),
        Map.entry(TOOL_WORN, "Your tool would break before the end"),
        Map.entry(TOO_FAR, "That track is too far away"),
        Map.entry(NO_CONNECTION, "This track has no curve or long straight to clear"),
        Map.entry(NOTHING, "Nothing to clear on this track")
    );

    private ClearMessages() {
    }

    public static MutableComponent component(String key, Object... arguments) {
        String fallback = FALLBACKS.get(key);
        return fallback == null ? Component.translatable(key, arguments) : Component.translatableWithFallback(key, fallback, arguments);
    }

    public static MutableComponent counted(String key, String oneKey, int count) {
        return component(count == 1 ? oneKey : key, Integer.toString(count));
    }

    public static MutableComponent refusal(BlockMiner.Problem problem) {
        return switch (problem.refusal()) {
            case WRONG_TOOL -> component(WRONG_TOOL);
            case TOOL_WORN -> component(TOOL_WORN);
            case PROTECTED -> {
                String key = "create.track." + Messages.BORE_PROTECTED_AT;
                BlockPos pos = problem.pos();
                String fallback = Messages.fallback(key);
                Object[] arguments = {problem.state().getBlock().getName(), pos.getX() + ", " + pos.getY() + ", " + pos.getZ()};
                yield fallback == null ? Component.translatable(key, arguments) : Component.translatableWithFallback(key, fallback, arguments);
            }
            default -> Messages.overlay(problem.refusal().message());
        };
    }
}
