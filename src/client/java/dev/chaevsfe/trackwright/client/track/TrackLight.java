package dev.chaevsfe.trackwright.client.track;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;

import java.util.function.ToIntFunction;

public final class TrackLight {
    private static final int REACH = 2;

    private TrackLight() {
    }

    public static int insideSolidBlock(BlockAndTintGetter level, BlockPos pos, int light, ToIntFunction<BlockPos> lightAt) {
        if (!level.getBlockState(pos).isSolidRender(level, pos)) {
            return light;
        }
        int brightest = light;
        for (Direction direction : Direction.values()) {
            for (int step = 1; step <= REACH; step++) {
                BlockPos open = pos.relative(direction, step);
                if (!level.getBlockState(open).isSolidRender(level, open)) {
                    brightest = brighter(brightest, lightAt.applyAsInt(open));
                    break;
                }
            }
        }
        return brightest;
    }

    private static int brighter(int first, int second) {
        return LightTexture.pack(Math.max(LightTexture.block(first), LightTexture.block(second)), Math.max(LightTexture.sky(first), LightTexture.sky(second)));
    }
}
