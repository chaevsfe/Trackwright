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
        Trackwright.LOGGER.info("Trackwright server settings: straightThroughBlocks={}, maxConnectionLength={}, protectTrainPassengers={}",
            settings.straightThroughBlocks(), settings.maxConnectionLength(), settings.protectTrainPassengers());
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
        TrackwrightSettings settings = new TrackwrightSettings(straight, length, protect);
        if (contents.writable() && !complete) {
            JsonObject known = new JsonObject();
            known.addProperty(STRAIGHT_THROUGH_BLOCKS, settings.straightThroughBlocks());
            known.addProperty(MAX_CONNECTION_LENGTH, length);
            known.addProperty(PROTECT_TRAIN_PASSENGERS, settings.protectTrainPassengers());
            JsonConfigFile.write(path, known, json);
        }
        return settings;
    }
}
