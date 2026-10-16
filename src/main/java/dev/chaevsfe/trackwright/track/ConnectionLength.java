package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.infrastructure.config.AllConfigs;
import com.zurrtum.create.infrastructure.config.CServer;
import dev.chaevsfe.trackwright.config.Sides;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;

public final class ConnectionLength {
    private ConnectionLength() {
    }

    public static Object raise(Object configValue, Object value) {
        CServer server = AllConfigs.server();
        if (server == null || configValue != server.trains.maxTrackPlacementLength || !(value instanceof Integer createLength)) {
            return value;
        }
        TrackwrightSettings settings = Sides.forCurrentThread();
        if (settings == null || settings.maxConnectionLength() <= createLength) {
            return value;
        }
        return settings.maxConnectionLength();
    }
}
