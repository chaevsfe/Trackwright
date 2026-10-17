package dev.chaevsfe.trackwright.config;

import com.google.gson.JsonObject;
import dev.chaevsfe.trackwright.Trackwright;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class ServerConfig {
    public static final String FILE_NAME = "trackwright-server.json";
    public static final String STRAIGHT_THROUGH_BLOCKS = "straightThroughBlocks";
    public static final String MAX_CONNECTION_LENGTH = "maxConnectionLength";
    public static final String PROTECT_TRAIN_PASSENGERS = "protectTrainPassengers";
    public static final String BORE_THROUGH_BLOCKS = "boreThroughBlocks";
    public static final String BORE_HEADROOM = "boreHeadroom";
    public static final String BORE_LAVA_CHECK = "boreLavaCheck";
    public static final String BORE_MAX_HARDNESS = "boreMaxHardness";

    private static volatile TrackwrightSettings current;

    private ServerConfig() {
    }

    public static TrackwrightSettings get() {
        TrackwrightSettings settings = current;
        if (settings == null) {
            synchronized (ServerConfig.class) {
                settings = current;
                if (settings == null) {
                    settings = load(path());
                    current = settings;
                }
            }
        }
        return settings;
    }

    public static TrackwrightSettings reload() {
        TrackwrightSettings settings = load(path());
        synchronized (ServerConfig.class) {
            current = settings;
        }
        Trackwright.LOGGER.info("Trackwright server settings: straightThroughBlocks={}, maxConnectionLength={}, protectTrainPassengers={}, boreThroughBlocks={}, boreHeadroom={}, boreLavaCheck={}, boreMaxHardness={}",
            settings.straightThroughBlocks(), settings.maxConnectionLength(), settings.protectTrainPassengers(), settings.boreThroughBlocks(),
            settings.boreHeadroom(), settings.boreLavaCheck(), settings.boreMaxHardness());
        return settings;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static TrackwrightSettings load(Path path) {
        JsonConfigFile.Contents contents = JsonConfigFile.read(path);
        JsonObject json = contents.json();
        TrackwrightSettings defaults = TrackwrightSettings.DEFAULTS;
        boolean complete = true;
        Boolean straight = JsonConfigFile.readBoolean(json, STRAIGHT_THROUGH_BLOCKS);
        Integer length = JsonConfigFile.readInt(json, MAX_CONNECTION_LENGTH);
        Boolean protect = JsonConfigFile.readBoolean(json, PROTECT_TRAIN_PASSENGERS);
        String boreText = JsonConfigFile.readString(json, BORE_THROUGH_BLOCKS);
        Integer headroom = JsonConfigFile.readInt(json, BORE_HEADROOM);
        Boolean lava = JsonConfigFile.readBoolean(json, BORE_LAVA_CHECK);
        Double hardness = JsonConfigFile.readDouble(json, BORE_MAX_HARDNESS);
        if (straight == null) {
            straight = defaults.straightThroughBlocks();
            complete = false;
        }
        if (length == null) {
            length = defaults.maxConnectionLength();
            complete = false;
        } else if (length != TrackwrightSettings.clampLength(length)) {
            Trackwright.LOGGER.warn("{} {} is outside {}..{}, using {}", MAX_CONNECTION_LENGTH, length,
                TrackwrightSettings.MIN_CONNECTION_LENGTH, TrackwrightSettings.MAX_CONNECTION_LENGTH, TrackwrightSettings.clampLength(length));
        }
        if (protect == null) {
            protect = defaults.protectTrainPassengers();
            complete = false;
        }
        BoreMode bore;
        if (boreText == null) {
            bore = defaults.boreThroughBlocks();
            complete = false;
        } else {
            bore = BoreMode.byId(boreText);
            if (bore == null) {
                Trackwright.LOGGER.warn("{} \"{}\" is not one of off, survival, creative-only, using off", BORE_THROUGH_BLOCKS, boreText);
                bore = BoreMode.OFF;
            }
        }
        if (headroom == null) {
            headroom = defaults.boreHeadroom();
            complete = false;
        } else if (headroom != TrackwrightSettings.clampHeadroom(headroom)) {
            Trackwright.LOGGER.warn("{} {} is outside 0..{}, using {}", BORE_HEADROOM, headroom, TrackwrightSettings.MAX_BORE_HEADROOM,
                TrackwrightSettings.clampHeadroom(headroom));
        }
        if (lava == null) {
            lava = defaults.boreLavaCheck();
            complete = false;
        }
        if (hardness == null) {
            hardness = defaults.boreMaxHardness();
            complete = false;
        } else if (hardness < 0) {
            Trackwright.LOGGER.warn("{} {} is below 0, using 0 (no limit)", BORE_MAX_HARDNESS, hardness);
        }
        TrackwrightSettings settings = new TrackwrightSettings(straight, length, protect, bore, headroom, lava, hardness);
        if (contents.writable() && !complete) {
            JsonObject known = new JsonObject();
            known.addProperty(STRAIGHT_THROUGH_BLOCKS, settings.straightThroughBlocks());
            known.addProperty(MAX_CONNECTION_LENGTH, length);
            known.addProperty(PROTECT_TRAIN_PASSENGERS, settings.protectTrainPassengers());
            known.addProperty(BORE_THROUGH_BLOCKS, boreText != null ? boreText : settings.boreThroughBlocks().id());
            known.addProperty(BORE_HEADROOM, headroom);
            known.addProperty(BORE_LAVA_CHECK, settings.boreLavaCheck());
            known.addProperty(BORE_MAX_HARDNESS, hardness);
            JsonConfigFile.write(path, known, json);
        }
        return settings;
    }
}
