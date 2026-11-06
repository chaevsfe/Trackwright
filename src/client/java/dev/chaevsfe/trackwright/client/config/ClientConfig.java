package dev.chaevsfe.trackwright.client.config;

import com.zurrtum.create.catnip.config.Builder;
import dev.chaevsfe.trackwright.Trackwright;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public final class ClientConfig {
    public static final String FILE_NAME = "trackwright-client.json";
    public static final String ALWAYS_SHOW_PLACEMENT_SQUARES = "alwaysShowPlacementSquares";
    public static final String PREVIEW_RANGE = "previewRange";
    public static final String PLANTS_COUNT_AS_GROUND = "plantsCountAsGround";
    public static final String SHOW_HEIGHT_HINTS = "showHeightHints";

    private static final String TYPE = "client";

    private static volatile @Nullable ClientConfigValues values;
    private static volatile ClientSettings fromFile = ClientSettings.DEFAULTS;

    private ClientConfig() {
    }

    public static ClientSettings get() {
        ClientConfigValues live = values;
        return live == null ? fromFile : live.settings();
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            ClientConfigFile.Prepared prepared = ClientConfigFile.prepare(path);
            fromFile = prepared.settings();
            if (prepared.ready()) {
                values = Builder.create(ClientConfigValues::new, Trackwright.MOD_ID, TYPE, true);
            }
        } catch (RuntimeException e) {
            Trackwright.LOGGER.warn("Could not load {}, the settings menu cannot change it this session: {}", path, e.toString());
        }
        ClientSettings settings = get();
        Trackwright.LOGGER.info("Trackwright client settings: alwaysShowPlacementSquares={}, previewRange={}, plantsCountAsGround={}, showHeightHints={}",
            settings.alwaysShowPlacementSquares(), settings.previewRange(), settings.plantsCountAsGround(), settings.showHeightHints());
    }
}
