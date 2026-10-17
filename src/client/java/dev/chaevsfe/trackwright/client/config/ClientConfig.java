package dev.chaevsfe.trackwright.client.config;

import com.google.gson.JsonObject;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.JsonConfigFile;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class ClientConfig {
    public static final String FILE_NAME = "trackwright-client.json";
    public static final String ALWAYS_SHOW_PLACEMENT_SQUARES = "alwaysShowPlacementSquares";
    public static final String PREVIEW_RANGE = "previewRange";
    public static final String PLANTS_COUNT_AS_GROUND = "plantsCountAsGround";

    private static volatile ClientSettings current = ClientSettings.DEFAULTS;

    private ClientConfig() {
    }

    public static ClientSettings get() {
        return current;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        JsonConfigFile.Contents contents = JsonConfigFile.read(path);
        JsonObject json = contents.json();
        ClientSettings defaults = ClientSettings.DEFAULTS;
        boolean complete = true;
        Boolean squares = JsonConfigFile.readBoolean(json, ALWAYS_SHOW_PLACEMENT_SQUARES);
        Integer range = JsonConfigFile.readInt(json, PREVIEW_RANGE);
        Boolean plants = JsonConfigFile.readBoolean(json, PLANTS_COUNT_AS_GROUND);
        if (squares == null) {
            squares = defaults.alwaysShowPlacementSquares();
            complete = false;
        }
        if (range == null) {
            range = defaults.previewRange();
            complete = false;
        } else if (range != ClientSettings.clampRange(range)) {
            Trackwright.LOGGER.warn("{} {} is outside 0..{}, using {}", PREVIEW_RANGE, range, ClientSettings.MAX_PREVIEW_RANGE, ClientSettings.clampRange(range));
        }
        if (plants == null) {
            plants = defaults.plantsCountAsGround();
            complete = false;
        }
        ClientSettings settings = new ClientSettings(squares, range, plants);
        if (contents.writable() && !complete) {
            JsonObject known = new JsonObject();
            known.addProperty(ALWAYS_SHOW_PLACEMENT_SQUARES, settings.alwaysShowPlacementSquares());
            known.addProperty(PREVIEW_RANGE, range);
            known.addProperty(PLANTS_COUNT_AS_GROUND, settings.plantsCountAsGround());
            JsonConfigFile.write(path, known, json);
        }
        current = settings;
        Trackwright.LOGGER.info("Trackwright client settings: alwaysShowPlacementSquares={}, previewRange={}, plantsCountAsGround={}",
            settings.alwaysShowPlacementSquares(), settings.previewRange(), settings.plantsCountAsGround());
    }
}
