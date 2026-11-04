package dev.chaevsfe.trackwright.client.input;

import com.zurrtum.create.client.content.trains.track.TrackBlockOutline;
import com.zurrtum.create.client.content.trains.track.TrackBlockOutline.BezierPointSelection;
import dev.chaevsfe.trackwright.client.guide.ClearBedPreview;
import dev.chaevsfe.trackwright.client.guide.RadiusLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class UseItemHooks {
    private UseItemHooks() {
    }

    public static boolean beforeUseItem(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.level == null || mc.hitResult == null) {
            return false;
        }
        if (CreateClickFirst.contraptionHit(mc)) {
            return false;
        }
        BezierPointSelection curve = TrackBlockOutline.result;
        if (curve == null && RadiusLock.beforeUseItem(mc, player)) {
            return true;
        }
        return ClearBedPreview.beforeUseItem(mc, player, curve);
    }
}
