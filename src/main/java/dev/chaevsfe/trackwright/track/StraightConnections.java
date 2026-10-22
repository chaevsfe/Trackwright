package dev.chaevsfe.trackwright.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class StraightConnections {
    private static final double TOLERANCE = 1 / 64.0;
    private static final double FLAT = 1 / 16.0;
    private static final int SCAN_REACH = TrackwrightSettings.MAX_CONNECTION_LENGTH;

    private StraightConnections() {
    }

    public static boolean isFlatStraight(@Nullable BezierConnection connection) {
        if (connection == null) {
            return false;
        }
        Vec3 start1 = connection.starts.getFirst();
        Vec3 start2 = connection.starts.getSecond();
        if (Math.abs(start1.y - start2.y) > FLAT) {
            return false;
        }
        Vec3 axis1 = connection.axes.getFirst();
        Vec3 axis2 = connection.axes.getSecond();
        if (Math.abs(axis1.y) > TOLERANCE || Math.abs(axis2.y) > TOLERANCE) {
            return false;
        }
        if (connection.normals.getFirst().distanceTo(connection.normals.getSecond()) > TOLERANCE) {
            return false;
        }
        Vec3 heading1 = horizontalUnit(axis1);
        Vec3 heading2 = horizontalUnit(axis2);
        if (heading1 == null || heading2 == null || heading1.distanceTo(heading2.scale(-1)) > TOLERANCE) {
            return false;
        }
        double dx = start2.x - start1.x;
        double dz = start2.z - start1.z;
        if (dx * heading1.x + dz * heading1.z < TOLERANCE) {
            return false;
        }
        return Math.abs(dx * heading1.z - dz * heading1.x) < TOLERANCE && Math.abs(dx * heading2.z - dz * heading2.x) < TOLERANCE;
    }

    private static @Nullable Vec3 horizontalUnit(Vec3 vector) {
        double length = Math.sqrt(vector.x * vector.x + vector.z * vector.z);
        if (length < TOLERANCE) {
            return null;
        }
        return new Vec3(vector.x / length, 0, vector.z / length);
    }

    public static @Nullable Line line(BlockPos origin, Vec3 axis) {
        if (Math.abs(axis.y) > TOLERANCE) {
            return null;
        }
        long stepX = Math.round(axis.x);
        long stepZ = Math.round(axis.z);
        if (Math.abs(axis.x - stepX) > TOLERANCE || Math.abs(axis.z - stepZ) > TOLERANCE) {
            return null;
        }
        if (Math.abs(stepX) > 1 || Math.abs(stepZ) > 1 || stepX == 0 && stepZ == 0) {
            return null;
        }
        return new Line(origin, (int) stepX, (int) stepZ);
    }

    public static boolean overlapsStraight(Level level, Line line, int from, int to, @Nullable BlockPos endA, @Nullable BlockPos endB) {
        if (from > to) {
            return false;
        }
        for (int k = from - SCAN_REACH; k <= to + SCAN_REACH; k++) {
            BlockPos pos = line.at(k);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ITrackBlock) || !state.hasProperty(TrackBlock.HAS_BE) || !state.getValue(TrackBlock.HAS_BE)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof TrackBlockEntity track)) {
                continue;
            }
            for (BezierConnection connection : track.getConnections().values()) {
                if (!isFlatStraight(connection)) {
                    continue;
                }
                BlockPos other = connection.getKey();
                if (endA != null && endB != null && (pos.equals(endA) && other.equals(endB) || pos.equals(endB) && other.equals(endA))) {
                    continue;
                }
                Integer otherStep = line.stepOf(other);
                if (otherStep == null) {
                    continue;
                }
                int low = Math.min(k, otherStep);
                int high = Math.max(k, otherStep);
                if (Math.max(low + 1, from) <= Math.min(high - 1, to)) {
                    return true;
                }
            }
        }
        return false;
    }

    public record Line(BlockPos origin, int stepX, int stepZ) {
        public BlockPos at(int step) {
            return origin.offset(stepX * step, 0, stepZ * step);
        }

        public @Nullable Integer stepOf(BlockPos pos) {
            if (pos.getY() != origin.getY()) {
                return null;
            }
            int dx = pos.getX() - origin.getX();
            int dz = pos.getZ() - origin.getZ();
            int step = stepX != 0 ? dx * stepX : dz * stepZ;
            if (dx != stepX * step || dz != stepZ * step) {
                return null;
            }
            return step;
        }
    }
}
