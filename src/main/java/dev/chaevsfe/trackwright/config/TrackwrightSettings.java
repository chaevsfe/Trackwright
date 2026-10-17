package dev.chaevsfe.trackwright.config;

public record TrackwrightSettings(
    boolean straightThroughBlocks,
    int maxConnectionLength,
    boolean protectTrainPassengers,
    BoreMode boreThroughBlocks,
    int boreHeadroom,
    boolean boreLavaCheck,
    double boreMaxHardness
) {
    public static final int MIN_CONNECTION_LENGTH = 16;
    public static final int MAX_CONNECTION_LENGTH = 128;
    public static final int MAX_BORE_HEADROOM = 3;
    public static final TrackwrightSettings DEFAULTS = new TrackwrightSettings(true, MAX_CONNECTION_LENGTH, true, BoreMode.SURVIVAL, 1, true, 0);

    public TrackwrightSettings {
        maxConnectionLength = clampLength(maxConnectionLength);
        boreThroughBlocks = boreThroughBlocks == null ? BoreMode.OFF : boreThroughBlocks;
        boreHeadroom = clampHeadroom(boreHeadroom);
        boreMaxHardness = clampHardness(boreMaxHardness);
    }

    public static int clampLength(int length) {
        return Math.max(MIN_CONNECTION_LENGTH, Math.min(MAX_CONNECTION_LENGTH, length));
    }

    public static int clampHeadroom(int headroom) {
        return Math.max(0, Math.min(MAX_BORE_HEADROOM, headroom));
    }

    public static double clampHardness(double hardness) {
        return Double.isFinite(hardness) && hardness > 0 ? hardness : 0;
    }
}
