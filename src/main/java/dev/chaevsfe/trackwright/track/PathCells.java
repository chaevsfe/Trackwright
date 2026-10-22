package dev.chaevsfe.trackwright.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PathCells {
    private static final double RAIL_LIFT = 0.25;

    private PathCells() {
    }

    public record Ends(List<BlockPos> first, List<BlockPos> second, Set<BlockPos> starts) {
    }

    public record Body(List<BlockPos> cells, Map<BlockPos, BlockPos> beds) {
        public static final Body NONE = new Body(List.of(), Map.of());
    }

    public static boolean blocks(BlockState state) {
        return !state.canBeReplaced() && !state.is(BlockTags.FLOWERS) && !(state.getBlock() instanceof ITrackBlock);
    }

    public static Ends ends(PlacementInfo info, BlockPos pos1, BlockPos pos2, Vec3 axis1, Vec3 axis2) {
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        boolean curve = extension.trackwright$curve() != null;
        Set<BlockPos> starts = new HashSet<>();
        List<BlockPos> first = walk(pos1, axis1, extension.trackwright$end1Extent(), curve, pos1, pos2, starts);
        List<BlockPos> second = walk(pos2, axis2, extension.trackwright$end2Extent(), curve, pos1, pos2, starts);
        return new Ends(first, second, starts);
    }

    private static List<BlockPos> walk(BlockPos origin, Vec3 axis, int extent, boolean curve, BlockPos pos1, BlockPos pos2, Set<BlockPos> starts) {
        int last = curve ? extent : extent - 1;
        List<BlockPos> cells = new ArrayList<>();
        for (int i = 1; i <= last; i++) {
            BlockPos pos = origin.offset(BlockPos.containing(axis.scale(i)));
            if (!pos.equals(pos1) && !pos.equals(pos2)) {
                cells.add(pos);
                if (curve && i == extent) {
                    starts.add(pos);
                }
            }
        }
        return cells;
    }

    public static Body body(BezierConnection curve, Set<BlockPos> excluded) {
        BlockPos origin = curve.bePositions.getFirst();
        BlockPos other = curve.bePositions.getSecond();
        Set<BlockPos> cells = new LinkedHashSet<>();
        Map<BlockPos, BlockPos> beds = new HashMap<>();
        for (Map.Entry<Pair<Integer, Integer>, Double> entry : curve.rasterise().entrySet()) {
            int dx = entry.getKey().getFirst();
            int dz = entry.getKey().getSecond();
            double y = entry.getValue();
            BlockPos pos = new BlockPos(dx, Mth.floor(y), dz).offset(origin).above(1);
            if (pos.equals(origin) || pos.equals(other) || excluded.contains(pos)) {
                continue;
            }
            cells.add(pos);
            BlockPos bed = new BlockPos(dx, Mth.floor(y + RAIL_LIFT), dz).offset(origin);
            if (bed.getY() < pos.getY() && !bed.equals(origin) && !bed.equals(other) && !excluded.contains(bed)) {
                beds.put(pos, bed);
            }
        }
        List<BlockPos> ordered = new ArrayList<>(cells);
        ordered.sort(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(origin))
            .thenComparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getZ));
        return new Body(ordered, beds);
    }
}
