package dev.chaevsfe.trackwright.client.guide;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.trains.track.ITrackBlock;
import dev.chaevsfe.trackwright.client.config.ClientConfig;
import dev.chaevsfe.trackwright.client.config.ClientSettings;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class AimHelp {
    private static final int SETTLE_TICKS = 4;

    private static int walkCloser;
    private static boolean farAim;
    private static @Nullable BlockPos settlePos;
    private static int settleTicks;

    private AimHelp() {
    }

    public static void reset() {
        walkCloser = 0;
        farAim = false;
        settlePos = null;
        settleTicks = 0;
    }

    public static @Nullable HitResult aim(Minecraft mc, @Nullable HitResult real) {
        walkCloser = 0;
        farAim = false;
        LocalPlayer player = mc.player;
        if (player == null || !holdsSelection(player)) {
            settle(null);
            return real;
        }
        ClientSettings settings = ClientConfig.get();
        HitResult hit = real;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            HitResult far = player.pick(Math.max(farRange(), settings.previewRange()), 1.0f, false);
            if (far.getType() == HitResult.Type.BLOCK) {
                double distance = player.getEyePosition().distanceTo(far.getLocation());
                walkCloser = Math.max(1, Mth.ceil(distance - player.blockInteractionRange()));
                if (distance <= settings.previewRange()) {
                    hit = far;
                    farAim = true;
                }
            }
        }
        if (settings.plantsCountAsGround() && hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            hit = groundUnderPlant(player.level(), block);
        }
        settle(farAim && hit instanceof BlockHitResult block ? block.getBlockPos() : null);
        return hit;
    }

    public static int walkCloser() {
        return walkCloser;
    }

    public static boolean deepChecksAllowed() {
        return !farAim || settleTicks >= SETTLE_TICKS;
    }

    public static Direction faceForSquares(Direction face) {
        return deepChecksAllowed() ? face : Direction.DOWN;
    }

    static double farRange() {
        Integer limit = GuideLines.connectionLimit();
        return limit == null ? TrackwrightSettings.MAX_CONNECTION_LENGTH : Math.min(limit, TrackwrightSettings.MAX_CONNECTION_LENGTH);
    }

    static boolean holdsSelection(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(AllItemTags.TRACKS.tag)) {
            stack = player.getOffhandItem();
        }
        return stack.is(AllItemTags.TRACKS.tag) && stack.has(AllDataComponents.TRACK_CONNECTING_FROM);
    }

    private static BlockHitResult groundUnderPlant(Level level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ITrackBlock || !state.canBeReplaced()) {
            return hit;
        }
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (ground.getBlock() instanceof ITrackBlock || ground.canBeReplaced()) {
            return hit;
        }
        return new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), Direction.UP, below, false);
    }

    private static void settle(@Nullable BlockPos far) {
        if (far == null) {
            settlePos = null;
            settleTicks = 0;
        } else if (far.equals(settlePos)) {
            settleTicks++;
        } else {
            settlePos = far.immutable();
            settleTicks = 0;
        }
    }
}
