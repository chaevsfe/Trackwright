package dev.chaevsfe.trackwright.client.mixin;

import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrackPlacement.class)
public interface TrackPlacementAccessor {
    @Accessor("hoveringAngle")
    static int trackwright$hoveringAngle() {
        throw new AssertionError();
    }

    @Accessor("hoveringAngle")
    static void trackwright$setHoveringAngle(int angle) {
        throw new AssertionError();
    }

    @Accessor("lastItem")
    static ItemStack trackwright$lastItem() {
        throw new AssertionError();
    }

    @Accessor("lastItem")
    static void trackwright$setLastItem(ItemStack stack) {
        throw new AssertionError();
    }
}
