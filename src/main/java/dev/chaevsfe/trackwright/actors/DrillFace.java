package dev.chaevsfe.trackwright.actors;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.drill.DrillMovementBehaviour;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import dev.chaevsfe.trackwright.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DrillFace {
    public static final String QUEUE = "TrackwrightFaceQueue";
    private static final String BREAKING_POS = "BreakingPos";
    private static final String WAITING_TICKS = "WaitingTicks";
    private static final String PROGRESS = "Progress";
    private static final String PREVIOUS = "TrackwrightFacePrevious";
    private static final double REACH = 0.45;
    private static final double[] ACROSS = {-REACH, -REACH / 2, 0, REACH / 2, REACH};
    private static final double STEP = 0.25;
    private static final double MAX_SWEEP = 4.0;
    private static final double STILL_SQR = 0.1 * 0.1;
    private static final double MAX_DISTANCE_SQR = 3.0 * 3.0;

    private DrillFace() {
    }

    public static boolean applies(Object behaviour, MovementContext context) {
        return behaviour instanceof DrillMovementBehaviour && context.contraption instanceof CarriageContraption
            && context.world != null && !context.world.isClientSide() && context.position != null && context.rotation != null
            && context.state.hasProperty(DirectionalKineticBlock.FACING) && ServerConfig.get().trainDrillsFullFace();
    }

    public static void afterVisit(BlockBreakingMovementBehaviour behaviour, MovementContext context, BlockPos entered) {
        if (!applies(behaviour, context)) {
            return;
        }
        CompoundTag data = context.data;
        Vec3 point = context.position;
        Vec3 from = previous(data);
        boolean continuous = from != null && from.distanceTo(point) <= MAX_SWEEP
            && (from.distanceToSqr(point) < STILL_SQR || point.subtract(from).dot(context.motion) > 0);
        data.putLongArray(PREVIOUS, new long[]{
            Double.doubleToLongBits(point.x), Double.doubleToLongBits(point.y), Double.doubleToLongBits(point.z)
        });
        Set<BlockPos> merged = new LinkedHashSet<>(cells(context, entered, continuous ? from : null));
        if (continuous) {
            for (long cell : data.getLongArray(QUEUE)) {
                BlockPos pos = BlockPos.of(cell);
                if (!pos.equals(entered)) {
                    merged.add(pos);
                }
            }
        }
        List<BlockPos> queue = new ArrayList<>();
        for (BlockPos cell : merged) {
            if (cell.distToCenterSqr(point) <= MAX_DISTANCE_SQR) {
                queue.add(cell);
            }
        }
        if (queue.isEmpty()) {
            clear(data);
            return;
        }
        queue.sort((x, y) -> Double.compare(x.distToCenterSqr(point), y.distToCenterSqr(point)));
        long[] packed = new long[queue.size()];
        for (int i = 0; i < packed.length; i++) {
            packed[i] = queue.get(i).asLong();
        }
        data.putLongArray(QUEUE, packed);
    }

    public static void afterTick(BlockBreakingMovementBehaviour behaviour, MovementContext context) {
        CompoundTag data = context.data;
        if (!data.contains(QUEUE)) {
            return;
        }
        if (!applies(behaviour, context)) {
            clear(data);
            return;
        }
        if (data.contains(WAITING_TICKS)) {
            return;
        }
        if (data.contains(BREAKING_POS)) {
            if (data.contains(PROGRESS) || !stuck(behaviour, context)) {
                return;
            }
            data.remove(BREAKING_POS);
        }
        long[] packed = data.getLongArray(QUEUE);
        Level level = context.world;
        int used = 0;
        BlockPos found = null;
        while (used < packed.length && found == null) {
            BlockPos cell = BlockPos.of(packed[used++]);
            if (cell.distToCenterSqr(context.position) > MAX_DISTANCE_SQR || !level.isLoaded(cell)) {
                continue;
            }
            BlockState state = level.getBlockState(cell);
            if (TrainActors.canBreak(behaviour, context, level, cell, state, behaviour.canBreak(level, cell, state))) {
                found = cell;
            }
        }
        if (used >= packed.length) {
            data.remove(QUEUE);
        } else {
            data.putLongArray(QUEUE, Arrays.copyOfRange(packed, used, packed.length));
        }
        if (found != null) {
            data.put(BREAKING_POS, NbtUtils.writeBlockPos(found));
            context.stall = true;
        }
    }

    private static boolean stuck(BlockBreakingMovementBehaviour behaviour, MovementContext context) {
        BlockPos pos = NbtUtils.readBlockPos(context.data, BREAKING_POS).orElse(null);
        Level level = context.world;
        if (pos == null || !level.isLoaded(pos)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        return !TrainActors.canBreak(behaviour, context, level, pos, state, behaviour.canBreak(level, pos, state));
    }

    private static @Nullable Vec3 previous(CompoundTag data) {
        long[] bits = data.getLongArray(PREVIOUS);
        if (bits.length != 3) {
            return null;
        }
        return new Vec3(Double.longBitsToDouble(bits[0]), Double.longBitsToDouble(bits[1]), Double.longBitsToDouble(bits[2]));
    }

    public static List<BlockPos> cells(MovementContext context, BlockPos entered, @Nullable Vec3 from) {
        Direction facing = context.state.getValue(DirectionalKineticBlock.FACING);
        Vec3 localFacing = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3i a = facing.getAxis() == Direction.Axis.Y ? Direction.EAST.getNormal() : Direction.UP.getNormal();
        Vec3 localA = Vec3.atLowerCornerOf(a);
        Vec3 localB = localFacing.cross(localA);
        Vec3 worldA = context.rotation.apply(localA).normalize();
        Vec3 worldB = context.rotation.apply(localB).normalize();
        Vec3 point = context.position;
        if (from == null || from.distanceTo(point) > MAX_SWEEP) {
            from = point;
        }
        int steps = Math.max(1, (int) Math.ceil(from.distanceTo(point) / STEP));
        Set<BlockPos> out = new LinkedHashSet<>();
        for (int s = 0; s <= steps; s++) {
            Vec3 plane = from.lerp(point, s / (double) steps);
            for (double u : ACROSS) {
                for (double v : ACROSS) {
                    BlockPos cell = BlockPos.containing(plane.add(worldA.scale(u)).add(worldB.scale(v)));
                    if (!cell.equals(entered)) {
                        out.add(cell);
                    }
                }
            }
        }
        List<BlockPos> list = new ArrayList<>(out);
        list.sort((x, y) -> Double.compare(x.distToCenterSqr(point), y.distToCenterSqr(point)));
        return list;
    }

    public static void clear(CompoundTag data) {
        data.remove(QUEUE);
    }

    public static void reset(CompoundTag data) {
        data.remove(QUEUE);
        data.remove(PREVIOUS);
    }
}
