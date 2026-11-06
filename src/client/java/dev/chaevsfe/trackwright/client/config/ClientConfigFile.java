package dev.chaevsfe.trackwright.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zurrtum.create.catnip.config.Builder;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.JsonConfigFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.BiFunction;

final class ClientConfigFile {
    private static final String VALUE = "value";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final String BROKEN_SUFFIX = ".broken";

    private ClientConfigFile() {
    }

    record Prepared(ClientSettings settings, boolean ready) {
    }

    static Prepared prepare(Path path) {
        if (Files.notExists(path)) {
            return new Prepared(ClientSettings.DEFAULTS, write(path, new JsonObject()));
        }
        String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            return setAside(path, e.toString());
        } catch (IOException e) {
            Trackwright.LOGGER.warn("Could not read {}, using the defaults: {}", path, e.toString());
            return new Prepared(ClientSettings.DEFAULTS, false);
        }
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(text);
        } catch (RuntimeException e) {
            return setAside(path, e.toString());
        }
        if (!parsed.isJsonObject()) {
            return setAside(path, "it is not a JSON object");
        }
        return migrate(path, parsed.getAsJsonObject());
    }

    private static Prepared migrate(Path path, JsonObject json) {
        JsonObject before = json.deepCopy();
        ClientSettings defaults = ClientSettings.DEFAULTS;
        boolean squares = settle(json, ClientConfig.ALWAYS_SHOW_PLACEMENT_SQUARES, defaults.alwaysShowPlacementSquares(), JsonConfigFile::readBoolean);
        int range = settle(json, ClientConfig.PREVIEW_RANGE, defaults.previewRange(), JsonConfigFile::readInt);
        boolean plants = settle(json, ClientConfig.PLANTS_COUNT_AS_GROUND, defaults.plantsCountAsGround(), JsonConfigFile::readBoolean);
        boolean heights = settle(json, ClientConfig.SHOW_HEIGHT_HINTS, defaults.showHeightHints(), JsonConfigFile::readBoolean);
        if (range != ClientSettings.clampRange(range)) {
            Trackwright.LOGGER.warn("{} {} is outside 0..{}, using {}", ClientConfig.PREVIEW_RANGE, range, ClientSettings.MAX_PREVIEW_RANGE, ClientSettings.clampRange(range));
        }
        boolean ready = json.equals(before) || write(path, json);
        return new Prepared(new ClientSettings(squares, range, plants, heights), ready);
    }

    private static <T> T settle(JsonObject json, String key, T fallback, BiFunction<JsonObject, String, @Nullable T> reader) {
        JsonElement entry = json.get(key);
        if (entry == null) {
            return fallback;
        }
        if (entry instanceof JsonObject holder) {
            if (!holder.has(VALUE)) {
                warnWrongType(key, entry, fallback);
                return fallback;
            }
            T stored = reader.apply(holder, VALUE);
            if (stored != null) {
                return stored;
            }
            warnWrongType(key, entry, fallback);
            holder.add(VALUE, Builder.GSON.toJsonTree(fallback));
            return fallback;
        }
        T flat = reader.apply(json, key);
        if (flat == null) {
            warnWrongType(key, entry, fallback);
        }
        T settled = flat == null ? fallback : flat;
        JsonObject holder = new JsonObject();
        holder.add(VALUE, Builder.GSON.toJsonTree(settled));
        json.add(key, holder);
        return settled;
    }

    private static void warnWrongType(String key, JsonElement entry, Object fallback) {
        Trackwright.LOGGER.warn("{} in {} has the wrong type ({}), using {}", key, ClientConfig.FILE_NAME, entry, fallback);
    }

    private static Prepared setAside(Path path, String reason) {
        try {
            Path aside = asideName(path);
            Files.move(path, aside);
            Trackwright.LOGGER.warn("Could not read {}, using the defaults and keeping the file as {}: {}", path, aside.getFileName(), reason);
            return new Prepared(ClientSettings.DEFAULTS, write(path, new JsonObject()));
        } catch (IOException e) {
            Trackwright.LOGGER.warn("Could not read {} ({}) or keep it aside, using the defaults: {}", path, reason, e.toString());
            return new Prepared(ClientSettings.DEFAULTS, false);
        }
    }

    private static Path asideName(Path path) {
        Path aside = path.resolveSibling(path.getFileName() + BROKEN_SUFFIX);
        for (int number = 1; Files.exists(aside); number++) {
            aside = path.resolveSibling(path.getFileName() + BROKEN_SUFFIX + "." + number);
        }
        return aside;
    }

    private static boolean write(Path path, JsonObject json) {
        Path temp = path.resolveSibling(path.getFileName() + TEMP_SUFFIX);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temp, Builder.GSON.toJson(json), StandardCharsets.UTF_8);
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            Trackwright.LOGGER.warn("Could not write {}: {}", path, e.toString());
            deleteQuietly(temp);
            return false;
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            Trackwright.LOGGER.warn("Could not delete {}: {}", path, e.toString());
        }
    }
}
