package dev.chaevsfe.trackwright.client.config;

public record ClientSettings(boolean alwaysShowPlacementSquares, int previewRange, boolean plantsCountAsGround, boolean showHeightHints) {
    public static final int MAX_PREVIEW_RANGE = 128;
    public static final ClientSettings DEFAULTS = new ClientSettings(false, 0, true, true);

    public ClientSettings {
        previewRange = clampRange(previewRange);
    }

    public static int clampRange(int range) {
        return Math.max(0, Math.min(MAX_PREVIEW_RANGE, range));
    }
}
