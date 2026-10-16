package dev.chaevsfe.trackwright.mixin;

import com.zurrtum.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TrackPlacement.PlacementInfo.class)
public abstract class PlacementInfoMixin implements PlacementInfoExtension {
    @Unique
    private @Nullable String trackwright$refusal;
    @Unique
    private boolean trackwright$bridged;

    @Override
    public @Nullable String trackwright$refusal() {
        return trackwright$refusal;
    }

    @Override
    public void trackwright$setRefusal(@Nullable String refusal) {
        trackwright$refusal = refusal;
    }

    @Override
    public boolean trackwright$bridged() {
        return trackwright$bridged;
    }

    @Override
    public void trackwright$setBridged(boolean bridged) {
        trackwright$bridged = bridged;
    }
}
