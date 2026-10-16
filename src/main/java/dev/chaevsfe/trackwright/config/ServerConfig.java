package dev.chaevsfe.trackwright.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.chaevsfe.trackwright.Trackwright;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ServerConfig {
    public static final String FILE_NAME = "trackwright-server.json";
    public static final String STRAIGHT_THROUGH_BLOCKS = "straightThroughBlocks";
    public static final String MAX_CONNECTION_LENGTH = "maxConnectionLength";
    public static final String PROTECT_TRAIN_PASSENGERS = "protectTrainPassengers";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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
        JsonObject json = new JsonObject();
        boolean writable = true;
        if (Files.isRegularFile(path)) {
            try {
                JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
                if (parsed.isJsonObject()) {
                    json = parsed.getAsJsonObject();
                } else {
                    Trackwright.LOGGER.warn("{} is not a JSON object, using the defaults", path);
                    writable = false;
                }
            } catch (IOException | RuntimeException e) {
                Trackwright.LOGGER.warn("Could not read {}, using the defaults: {}", path, e.toString());
                writable = false;
            }
        }
        TrackwrightSettings defaults = TrackwrightSettings.DEFAULTS;
        boolean complete = true;
        Boolean straight = readBoolean(json, STRAIGHT_THROUGH_BLOCKS);
        Integer length = readInt(json, MAX_CONNECTION_LENGTH);
        Boolean protect = readBoolean(json, PROTECT_TRAIN_PASSENGERS);
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
        if (writable && !complete) {
            write(path, json, settings, length);
        }
        return settings;
    }

    private static Boolean readBoolean(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value instanceof JsonPrimitive primitive && primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        return null;
    }

    private static Integer readInt(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value instanceof JsonPrimitive primitive && primitive.isNumber()) {
            double number = primitive.getAsDouble();
            if (number == Math.rint(number) && Math.abs(number) <= Integer.MAX_VALUE) {
                return (int) number;
            }
        }
        return null;
    }

    private static void write(Path path, JsonObject existing, TrackwrightSettings settings, int length) {
        JsonObject json = new JsonObject();
        json.addProperty(STRAIGHT_THROUGH_BLOCKS, settings.straightThroughBlocks());
        json.addProperty(MAX_CONNECTION_LENGTH, length);
        json.addProperty(PROTECT_TRAIN_PASSENGERS, settings.protectTrainPassengers());
        for (String key : existing.keySet()) {
            if (!json.has(key)) {
                json.add(key, existing.get(key));
            }
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Trackwright.LOGGER.warn("Could not write {}: {}", path, e.toString());
        }
    }
}
