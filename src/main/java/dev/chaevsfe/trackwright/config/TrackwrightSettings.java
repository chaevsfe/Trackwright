package dev.chaevsfe.trackwright.config;

public record TrackwrightSettings(boolean straightThroughBlocks, int maxConnectionLength, boolean protectTrainPassengers) {
    public static final int MIN_CONNECTION_LENGTH = 16;
    public static final int MAX_CONNECTION_LENGTH = 128;
    public static final TrackwrightSettings DEFAULTS = new TrackwrightSettings(true, MAX_CONNECTION_LENGTH, true);

    public TrackwrightSettings {
        maxConnectionLength = clampLength(maxConnectionLength);
    }

    public static int clampLength(int length) {
        return Math.max(MIN_CONNECTION_LENGTH, Math.min(MAX_CONNECTION_LENGTH, length));
    }
}
