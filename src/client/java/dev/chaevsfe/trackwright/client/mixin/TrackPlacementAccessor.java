package dev.chaevsfe.trackwright.client.mixin;

import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TrackPlacement.class)
public interface TrackPlacementAccessor {
    @Accessor("hoveringPos")
    static @Nullable BlockPos trackwright$hoveringPos() {
        throw new AssertionError();
    }

    @Accessor("hoveringPos")
    static void trackwright$setHoveringPos(@Nullable BlockPos pos) {
        throw new AssertionError();
    }

    @Accessor("hoveringMaxed")
    static boolean trackwright$hoveringMaxed() {
        throw new AssertionError();
    }

    @Accessor("hoveringMaxed")
    static void trackwright$setHoveringMaxed(boolean maxed) {
        throw new AssertionError();
    }

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

    @Accessor("hintPos")
    static void trackwright$setHintPos(@Nullable BlockPos pos) {
        throw new AssertionError();
    }
}
