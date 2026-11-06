package dev.chaevsfe.trackwright.client.config;

import com.zurrtum.create.catnip.config.ConfigBase;

final class ClientConfigValues extends ConfigBase {
    private final ConfigBool alwaysShowPlacementSquares = new SavedBool(
        ClientSettings.DEFAULTS.alwaysShowPlacementSquares(),
        ClientConfig.ALWAYS_SHOW_PLACEMENT_SQUARES,
        "Show Create's placement squares even when they are all one colour"
    );
    private final ConfigInt previewRange = new SavedInt(
        ClientSettings.DEFAULTS.previewRange(),
        0,
        ClientSettings.MAX_PREVIEW_RANGE,
        ClientConfig.PREVIEW_RANGE,
        "Preview a connection up to this many blocks away. 0 means within reach. Placing still needs reach"
    );
    private final ConfigBool plantsCountAsGround = new SavedBool(
        ClientSettings.DEFAULTS.plantsCountAsGround(),
        ClientConfig.PLANTS_COUNT_AS_GROUND,
        "Aiming at grass or flowers previews on the block below"
    );
    private final ConfigBool showHeightHints = new SavedBool(
        ClientSettings.DEFAULTS.showHeightHints(),
        ClientConfig.SHOW_HEIGHT_HINTS,
        "Show height markers while placing track"
    );

    @Override
    public String getName() {
        return "client";
    }

    ClientSettings settings() {
        return new ClientSettings(
            alwaysShowPlacementSquares.get(),
            previewRange.get(),
            plantsCountAsGround.get(),
            showHeightHints.get()
        );
    }

    private final class SavedBool extends ConfigBool {
        SavedBool(boolean current, String name, String comment) {
            super(name, current, comment);
        }

        @Override
        public void set(Boolean value) {
            try {
                super.set(value);
            } finally {
                ClientConfig.saveFromMenu(settings());
            }
        }
    }

    private final class SavedInt extends ConfigInt {
        SavedInt(int current, int min, int max, String name, String comment) {
            super(name, current, min, max, comment);
        }

        @Override
        public void set(Integer value) {
            try {
                super.set(value);
            } finally {
                ClientConfig.saveFromMenu(settings());
            }
        }
    }
}
