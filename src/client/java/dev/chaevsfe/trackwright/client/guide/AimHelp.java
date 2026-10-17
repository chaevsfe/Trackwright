package dev.chaevsfe.trackwright.client.guide;

import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.AllItemTags;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public final class AimHelp {
    private static int walkCloser;

    private AimHelp() {
    }

    public static void reset() {
        walkCloser = 0;
    }

    public static @Nullable HitResult aim(Minecraft mc, @Nullable HitResult real) {
        walkCloser = 0;
        LocalPlayer player = mc.player;
        if (player == null || !holdsSelection(player) || real != null && real.getType() != HitResult.Type.MISS) {
            return real;
        }
        HitResult far = player.pick(farRange(), 1.0f, false);
        if (far.getType() == HitResult.Type.BLOCK) {
            double distance = player.getEyePosition().distanceTo(far.getLocation());
            walkCloser = Math.max(1, Mth.ceil(distance - player.blockInteractionRange()));
        }
        return real;
    }

    public static int walkCloser() {
        return walkCloser;
    }

    static double farRange() {
        Integer limit = GuideLines.connectionLimit();
        return limit == null ? TrackwrightSettings.MAX_CONNECTION_LENGTH : Math.min(limit, TrackwrightSettings.MAX_CONNECTION_LENGTH);
    }

    static boolean holdsSelection(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(AllItemTags.TRACKS)) {
            stack = player.getOffhandItem();
        }
        return stack.is(AllItemTags.TRACKS) && stack.has(AllDataComponents.TRACK_CONNECTING_FROM);
    }
}
