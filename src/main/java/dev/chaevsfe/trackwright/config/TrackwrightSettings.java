package dev.chaevsfe.trackwright.config;

public record TrackwrightSettings(
    boolean straightThroughBlocks,
    int maxConnectionLength,
    boolean protectTrainPassengers,
    BoreMode boreThroughBlocks,
    int boreWidthMax,
    int boreHeadroomMax,
    BoreProfile boreProfileDefault,
    boolean boreLavaCheck,
    double boreMaxHardness,
    boolean refuseBlockedTrackEnds,
    boolean protectTrackEquipment,
    boolean rollersSkipAirPaving,
    boolean rollersClearPowderSnow,
    boolean rollersIgnoreFluids,
    boolean contraptionControlsSchedule,
    boolean trainDrillsFullFace,
    boolean rollersNeedGround,
    boolean rollerReshape
) {
    public static final int MIN_CONNECTION_LENGTH = 16;
    public static final int MAX_CONNECTION_LENGTH = 128;
    public static final int MAX_BORE_WIDTH = 9;
    public static final int MAX_BORE_HEADROOM = 6;
    public static final int LEGACY_HEADROOM_DEFAULT = 1;
    public static final TrackwrightSettings DEFAULTS = new TrackwrightSettings(
        true, MAX_CONNECTION_LENGTH, true, BoreMode.SURVIVAL, 5, 3, BoreProfile.DEFAULT, true, 0, true,
        true, true, false, false, true, true, false, true
    );

    public TrackwrightSettings {
        maxConnectionLength = clampLength(maxConnectionLength);
        boreThroughBlocks = boreThroughBlocks == null ? BoreMode.OFF : boreThroughBlocks;
        boreWidthMax = clampWidthMax(boreWidthMax);
        boreHeadroomMax = clampHeadroom(boreHeadroomMax);
        boreProfileDefault = (boreProfileDefault == null ? new BoreProfile(0, 0, 0) : boreProfileDefault).clamp(boreWidthMax, boreHeadroomMax);
        boreMaxHardness = clampHardness(boreMaxHardness);
    }

    public int boreHeadroom() {
        return boreProfileDefault.up();
    }

    public static int clampLength(int length) {
        return Math.max(MIN_CONNECTION_LENGTH, Math.min(MAX_CONNECTION_LENGTH, length));
    }

    public static int clampWidthMax(int width) {
        return Math.max(1, Math.min(MAX_BORE_WIDTH, width));
    }

    public static int clampHeadroom(int headroom) {
        return Math.max(0, Math.min(MAX_BORE_HEADROOM, headroom));
    }

    public static double clampHardness(double hardness) {
        return Double.isFinite(hardness) && hardness > 0 ? hardness : 0;
    }
}
