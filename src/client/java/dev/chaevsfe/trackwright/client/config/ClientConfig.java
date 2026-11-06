package dev.chaevsfe.trackwright.client.config;

import com.google.gson.JsonObject;
import com.zurrtum.create.catnip.config.Builder;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.JsonConfigFile;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public final class ClientConfig {
    public static final String FILE_NAME = "trackwright-client.json";
    public static final String ALWAYS_SHOW_PLACEMENT_SQUARES = "alwaysShowPlacementSquares";
    public static final String PREVIEW_RANGE = "previewRange";
    public static final String PLANTS_COUNT_AS_GROUND = "plantsCountAsGround";
    public static final String SHOW_HEIGHT_HINTS = "showHeightHints";

    private static final String MENU_TYPE = "client";

    private static volatile ClientSettings current = ClientSettings.DEFAULTS;
    private static volatile @Nullable ClientConfigValues menuValues;

    private ClientConfig() {
    }

    public static ClientSettings get() {
        ClientConfigValues live = menuValues;
        return live == null ? current : live.settings();
    }

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path path = configDir.resolve(FILE_NAME);
        JsonConfigFile.Contents contents = JsonConfigFile.read(path);
        JsonObject json = contents.json();
        ClientSettings defaults = ClientSettings.DEFAULTS;
        boolean complete = true;
        Boolean squares = JsonConfigFile.readBoolean(json, ALWAYS_SHOW_PLACEMENT_SQUARES);
        Integer range = JsonConfigFile.readInt(json, PREVIEW_RANGE);
        Boolean plants = JsonConfigFile.readBoolean(json, PLANTS_COUNT_AS_GROUND);
        Boolean heights = JsonConfigFile.readBoolean(json, SHOW_HEIGHT_HINTS);
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
        if (heights == null) {
            heights = defaults.showHeightHints();
            complete = false;
        }
        ClientSettings settings = new ClientSettings(squares, range, plants, heights);
        if (contents.writable() && !complete) {
            JsonObject known = new JsonObject();
            known.addProperty(ALWAYS_SHOW_PLACEMENT_SQUARES, settings.alwaysShowPlacementSquares());
            known.addProperty(PREVIEW_RANGE, range);
            known.addProperty(PLANTS_COUNT_AS_GROUND, settings.plantsCountAsGround());
            known.addProperty(SHOW_HEIGHT_HINTS, settings.showHeightHints());
            JsonConfigFile.write(path, known, json);
        }
        current = settings;
        offerToMenu(path, configDir.resolve(Trackwright.MOD_ID).resolve(MENU_TYPE + ".json"), settings);
        Trackwright.LOGGER.info("Trackwright client settings: alwaysShowPlacementSquares={}, previewRange={}, plantsCountAsGround={}, showHeightHints={}",
            settings.alwaysShowPlacementSquares(), settings.previewRange(), settings.plantsCountAsGround(), settings.showHeightHints());
    }

    static void saveFromMenu(ClientConfigValues values) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (values == menuValues) {
            ClientConfigFile.store(path, values.settings());
        } else {
            Trackwright.LOGGER.warn("The settings menu is off this session, so this change does nothing. Edit {} instead", path);
        }
    }

    private static void offerToMenu(Path path, Path menuCopy, ClientSettings settings) {
        try {
            if (!ClientConfigFile.canReplace(path)) {
                Trackwright.LOGGER.warn("{} or its folder cannot be written, the settings menu cannot change it this session", path);
            } else if (ClientConfigFile.writeMenuCopy(menuCopy, settings)) {
                ClientConfigValues values = Builder.create(ClientConfigValues::new, Trackwright.MOD_ID, MENU_TYPE);
                ClientSettings read = values.settings();
                if (read.equals(settings)) {
                    menuValues = values;
                } else {
                    Trackwright.LOGGER.warn("The settings menu read {} from {} but {} says {}. Using the file, and the menu is off this session", read, menuCopy, path, settings);
                }
            }
        } catch (RuntimeException e) {
            Trackwright.LOGGER.warn("The settings menu cannot change {} this session: {}", path, e.toString());
        }
    }
}
