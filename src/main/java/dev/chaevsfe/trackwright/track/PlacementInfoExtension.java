package dev.chaevsfe.trackwright.track;

import org.jspecify.annotations.Nullable;

public interface PlacementInfoExtension {
    @Nullable String trackwright$refusal();

    void trackwright$setRefusal(@Nullable String refusal);

    boolean trackwright$bridged();

    void trackwright$setBridged(boolean bridged);
}
