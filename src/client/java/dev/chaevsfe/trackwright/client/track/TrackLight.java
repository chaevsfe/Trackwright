package dev.chaevsfe.trackwright.client.track;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;

import java.util.function.ToIntFunction;

public final class TrackLight {
    private static final int REACH = 2;

    private TrackLight() {
    }

    public static int insideSolidBlock(BlockAndLightGetter level, BlockPos pos, int light, ToIntFunction<BlockPos> lightAt) {
        if (!level.getBlockState(pos).isSolidRender()) {
            return light;
        }
        int brightest = light;
        for (Direction direction : Direction.values()) {
            for (int step = 1; step <= REACH; step++) {
                BlockPos open = pos.relative(direction, step);
                if (!level.getBlockState(open).isSolidRender()) {
                    brightest = LightCoordsUtil.max(brightest, lightAt.applyAsInt(open));
                    break;
                }
            }
        }
        return brightest;
    }
}
