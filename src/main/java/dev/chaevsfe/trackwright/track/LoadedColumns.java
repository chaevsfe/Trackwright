package dev.chaevsfe.trackwright.track;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public final class LoadedColumns {
    private LoadedColumns() {
    }

    public static Map<Pair<Integer, Integer>, Double> filter(@Nullable Level level, BlockPos origin, Map<Pair<Integer, Integer>, Double> columns) {
        if (level == null) {
            return columns;
        }
        columns.keySet().removeIf(column -> !level.hasChunk(
            SectionPos.blockToSectionCoord(origin.getX() + column.getFirst()),
            SectionPos.blockToSectionCoord(origin.getZ() + column.getSecond())
        ));
        return columns;
    }
}
