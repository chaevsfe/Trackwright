package dev.chaevsfe.trackwright.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.chaevsfe.trackwright.Trackwright;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonConfigFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonConfigFile() {
    }

    public record Contents(JsonObject json, boolean writable) {
    }

    public static Contents read(Path path) {
        if (!Files.isRegularFile(path)) {
            return new Contents(new JsonObject(), true);
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (parsed.isJsonObject()) {
                return new Contents(parsed.getAsJsonObject(), true);
            }
            Trackwright.LOGGER.warn("{} is not a JSON object, using the defaults", path);
        } catch (IOException | RuntimeException e) {
            Trackwright.LOGGER.warn("Could not read {}, using the defaults: {}", path, e.toString());
        }
        return new Contents(new JsonObject(), false);
    }

    public static @Nullable Boolean readBoolean(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value instanceof JsonPrimitive primitive && primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        return null;
    }

    public static @Nullable Integer readInt(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value instanceof JsonPrimitive primitive && primitive.isNumber()) {
            double number = primitive.getAsDouble();
            if (number == Math.rint(number) && Math.abs(number) <= Integer.MAX_VALUE) {
                return (int) number;
            }
        }
        return null;
    }

    public static @Nullable String readString(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value instanceof JsonPrimitive primitive && primitive.isString()) {
            return primitive.getAsString();
        }
        return null;
    }

    public static @Nullable Double readDouble(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value instanceof JsonPrimitive primitive && primitive.isNumber()) {
            double number = primitive.getAsDouble();
            if (Double.isFinite(number)) {
                return number;
            }
        }
        return null;
    }

    public static void write(Path path, JsonObject known, JsonObject existing) {
        JsonObject json = known.deepCopy();
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
