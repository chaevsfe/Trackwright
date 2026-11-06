package dev.chaevsfe.trackwright.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zurrtum.create.catnip.config.Builder;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.JsonConfigFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

final class ClientConfigFile {
    private static final String VALUE = "value";
    private static final String NOTE = "note";
    private static final String NOTE_TEXT = "The config menu's copy of " + ClientConfig.FILE_NAME + ", replaced from it at every start. Edit that file instead.";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final String BROKEN_SUFFIX = ".broken";

    private ClientConfigFile() {
    }

    static boolean writeMenuCopy(Path menuCopy, ClientSettings settings) {
        JsonObject json = new JsonObject();
        json.addProperty(NOTE, NOTE_TEXT);
        for (Map.Entry<String, JsonElement> setting : known(settings).entrySet()) {
            JsonObject holder = new JsonObject();
            holder.add(VALUE, setting.getValue());
            json.add(setting.getKey(), holder);
        }
        return write(menuCopy, json);
    }

    static void store(Path path, ClientSettings settings) {
        JsonConfigFile.Contents contents = JsonConfigFile.read(path);
        if (!contents.writable() && !setAside(path)) {
            return;
        }
        JsonObject json = contents.json();
        for (Map.Entry<String, JsonElement> setting : known(settings).entrySet()) {
            json.add(setting.getKey(), setting.getValue());
        }
        write(path, json);
    }

    private static JsonObject known(ClientSettings settings) {
        JsonObject json = new JsonObject();
        json.addProperty(ClientConfig.ALWAYS_SHOW_PLACEMENT_SQUARES, settings.alwaysShowPlacementSquares());
        json.addProperty(ClientConfig.PREVIEW_RANGE, settings.previewRange());
        json.addProperty(ClientConfig.PLANTS_COUNT_AS_GROUND, settings.plantsCountAsGround());
        json.addProperty(ClientConfig.SHOW_HEIGHT_HINTS, settings.showHeightHints());
        return json;
    }

    private static boolean setAside(Path path) {
        try {
            Path aside = asideName(path);
            Files.move(path, aside);
            Trackwright.LOGGER.warn("Keeping the unreadable {} as {}", path, aside.getFileName());
            return true;
        } catch (IOException e) {
            Trackwright.LOGGER.warn("Could not keep the unreadable {} aside, the settings menu's change is not saved: {}", path, e.toString());
            return false;
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
        try {
            Path target = Files.exists(path) ? path.toRealPath() : path;
            Path temp = target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
            Files.createDirectories(target.getParent());
            try {
                Files.writeString(temp, Builder.GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                deleteQuietly(temp);
                throw e;
            }
            return true;
        } catch (IOException e) {
            Trackwright.LOGGER.warn("Could not write {}: {}", path, e.toString());
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
