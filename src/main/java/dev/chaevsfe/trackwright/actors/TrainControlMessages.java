package dev.chaevsfe.trackwright.actors;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;

public final class TrainControlMessages {
    public static final String UNASSIGNED = "trackwright.train_controls.unassigned";
    public static final String GONE = "trackwright.train_controls.gone";
    public static final String NOT_LOADED = "trackwright.train_controls.not_loaded";
    public static final String TOO_FAR = "trackwright.train_controls.too_far";
    public static final String TOO_FAR_CREATE = "trackwright.train_controls.too_far_create";
    public static final String CHANGED = "trackwright.train_controls.changed";
    public static final String TRAIN_GONE = "trackwright.train_controls.train_gone";
    public static final String NO_TRAIN = "trackwright.train_controls.no_train";

    private static final Map<String, String> FALLBACKS = Map.of(
        UNASSIGNED, "Train control %s has no control yet: sneak and right-click a Contraption Controls block on a train",
        GONE, "Train control %s is cleared: its train was disassembled",
        NOT_LOADED, "That carriage is not loaded near you",
        TOO_FAR, "Too far from that control: sit on its train or walk closer",
        TOO_FAR_CREATE, "Too far from that control: without Trackwright on the server the key reaches only as far as a click",
        CHANGED, "That control is not there any more: assign the key again",
        TRAIN_GONE, "That train is not there any more: assign the key again",
        NO_TRAIN, "Sit on a train or stand in one of its carriages to see its controls"
    );

    private TrainControlMessages() {
    }

    public static MutableComponent refusal(String key, Object... args) {
        String fallback = FALLBACKS.get(key);
        MutableComponent text = fallback == null ? Component.translatable(key, args) : Component.translatableWithFallback(key, fallback, args);
        return text.withStyle(ChatFormatting.RED);
    }
}
