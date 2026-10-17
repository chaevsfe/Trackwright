package dev.chaevsfe.trackwright.client.guide;

import com.zurrtum.create.content.trains.track.TrackPlacement;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import dev.chaevsfe.trackwright.client.mixin.TrackPlacementAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record PlacementCache(@Nullable PlacementInfo cached, @Nullable BlockPos hoveringPos, boolean hoveringMaxed, int hoveringAngle, @Nullable ItemStack lastItem) {
    public static PlacementCache save() {
        return new PlacementCache(
            TrackPlacement.cached,
            TrackPlacement.hoveringPos,
            TrackPlacement.hoveringMaxed,
            TrackPlacementAccessor.trackwright$hoveringAngle(),
            TrackPlacementAccessor.trackwright$lastItem()
        );
    }

    public void restore() {
        TrackPlacement.cached = cached;
        TrackPlacement.hoveringPos = hoveringPos;
        TrackPlacement.hoveringMaxed = hoveringMaxed;
        TrackPlacementAccessor.trackwright$setHoveringAngle(hoveringAngle);
        TrackPlacementAccessor.trackwright$setLastItem(lastItem);
    }
}
