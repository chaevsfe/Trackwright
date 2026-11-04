package dev.chaevsfe.trackwright.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ClearBedPlan(BlockPos track, BlockPos target, BorePlan bore, int water, long hash) {
    private static final long SEED = 0x9E3779B97F4A7C15L;

    public int count() {
        return bore.cells().size();
    }

    public boolean guessed() {
        return bore.guessed();
    }

    public static ClearBedPlan of(Level level, BlockPos track, BezierConnection connection, ItemStack tool, boolean creative, BoreProfile profile,
                                  TrackwrightSettings settings) {
        BlockPos origin = connection.bePositions.getFirst();
        BlockPos other = connection.bePositions.getSecond();
        PathCells.Body body = PathCells.body(connection, Set.of());
        Set<BlockPos> path = new LinkedHashSet<>(body.cells());
        Set<BlockPos> blocked = new HashSet<>();
        BlockPos unloaded = null;
        boolean unseen = false;
        List<BlockPos> beds = new ArrayList<>();
        for (BlockPos cell : body.cells()) {
            if (!loaded(level, cell)) {
                if (level.isClientSide()) {
                    unseen = true;
                } else if (unloaded == null) {
                    unloaded = cell;
                }
                continue;
            }
            if (PathCells.blocks(level.getBlockState(cell))) {
                blocked.add(cell);
                BlockPos bed = body.beds().get(cell);
                if (bed != null) {
                    beds.add(bed);
                }
            }
        }
        for (BlockPos bed : beds) {
            if (!loaded(level, bed)) {
                if (level.isClientSide()) {
                    unseen = true;
                } else if (unloaded == null) {
                    unloaded = bed;
                }
                continue;
            }
            if (path.add(bed) && PathCells.blocks(level.getBlockState(bed))) {
                blocked.add(bed);
            }
        }
        BoreBand.Builder line = new BoreBand.Builder().cell(origin).cells(body.cells()).cell(other).centre(origin);
        BoreBand.curvePoints(line, connection, origin);
        line.centre(other);
        BorePlan bore = BorePlan.of(level, tool, creative, new ArrayList<>(path), blocked, Set.of(), line.build(), profile, unloaded, unseen,
            settings, true);
        return new ClearBedPlan(track.immutable(), connection.getKey().immutable(), bore, waterNext(level, bore.cells()), hash(bore.cells()));
    }

    public static long hash(List<BlockPos> cells) {
        long hash = SEED * (cells.size() + 1);
        for (BlockPos cell : cells) {
            hash += HashCommon.mix(cell.asLong());
        }
        return hash;
    }

    private static int waterNext(Level level, List<BlockPos> cells) {
        Set<BlockPos> mined = new HashSet<>(cells);
        int touching = 0;
        for (BlockPos cell : cells) {
            for (Direction direction : Direction.values()) {
                BlockPos next = cell.relative(direction);
                if (!mined.contains(next) && loaded(level, next) && level.getFluidState(next).is(FluidTags.WATER)) {
                    touching++;
                    break;
                }
            }
        }
        return touching;
    }

    public static @Nullable BezierConnection pick(TrackBlockEntity track, Vec3 look) {
        Map<BlockPos, BezierConnection> connections = track.getConnections();
        BezierConnection best = null;
        double bestDot = Double.NEGATIVE_INFINITY;
        double lx = look.x;
        double lz = look.z;
        double length = Math.sqrt(lx * lx + lz * lz);
        for (BezierConnection connection : connections.values()) {
            Vec3 axis = connection.axes.getFirst();
            double ax = axis.x;
            double az = axis.z;
            double axisLength = Math.sqrt(ax * ax + az * az);
            double dot = length < 1.0E-6 || axisLength < 1.0E-6 ? 0 : (lx * ax + lz * az) / (length * axisLength);
            if (best == null || dot > bestDot) {
                best = connection;
                bestDot = dot;
            }
        }
        return best;
    }

    private static boolean loaded(Level level, BlockPos pos) {
        return level.isClientSide()
            ? level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))
            : level.isLoaded(pos);
    }
}
