package dev.chaevsfe.trackwright.client.guide;

import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.Nullable;

public final class ClearBedPreview {
    private ClearBedPreview() {
    }

    public static boolean beforeUseItem(Minecraft mc, LocalPlayer player, @Nullable BezierPointSelection curve) {
        return false;
    }
}
