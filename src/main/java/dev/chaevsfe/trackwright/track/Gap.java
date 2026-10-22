package dev.chaevsfe.trackwright.track;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record Gap(List<BlockPos> cells, BlockPos pos, BlockState state, boolean start) {
    public Gap {
        cells = List.copyOf(cells);
        pos = pos.immutable();
    }

    public static Gap of(Map<BlockPos, BlockState> blocked, Set<BlockPos> starts) {
        Map.Entry<BlockPos, BlockState> named = null;
        for (Map.Entry<BlockPos, BlockState> entry : blocked.entrySet()) {
            if (!starts.contains(entry.getKey())) {
                named = entry;
                break;
            }
        }
        boolean start = named == null;
        if (start) {
            named = blocked.entrySet().iterator().next();
        }
        return new Gap(List.copyOf(blocked.keySet()), named.getKey(), named.getValue(), start);
    }
}
