package dev.chaevsfe.trackwright.client.mixin;

import com.zurrtum.create.client.content.trains.track.TrackPlacementClient;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrackPlacementClient.class)
public interface TrackPlacementClientAccessor {
    @Accessor("hintPos")
    static void trackwright$setHintPos(@Nullable BlockPos pos) {
        throw new AssertionError();
    }
}
