package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackShape;
import com.zurrtum.create.infrastructure.component.ConnectingFrom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class UnloadedStart {
    private static final double TOLERANCE = 1.0E-3;

    private UnloadedStart() {
    }

    public static BlockState startState(Level level, BlockPos pos, BlockState found, ItemStack stack) {
        if (!level.isClientSide() || found.hasProperty(TrackBlock.HAS_BE) || level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
            return found;
        }
        ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null || !pos.equals(from.pos())) {
            return found;
        }
        BlockState madeUp = stateFor(stack, from.axis(), from.normal());
        return madeUp != null ? madeUp : found;
    }

    public static BlockState extentState(Level level, BlockPos pos, BlockState found, boolean firstEnd, BlockPos start, BlockState startState) {
        if (!firstEnd || !level.isClientSide() || !pos.equals(start) || !startState.hasProperty(TrackBlock.HAS_BE)
            || level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
            return found;
        }
        return startState;
    }

    public static @Nullable BlockState stateFor(ItemStack stack, Vec3 axis, Vec3 normal) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof TrackBlock block)) {
            return null;
        }
        TrackShape shape = shapeFor(axis, normal);
        if (shape == null) {
            return null;
        }
        return block.defaultBlockState().setValue(TrackBlock.SHAPE, shape).setValue(TrackBlock.HAS_BE, true);
    }

    public static @Nullable TrackShape shapeFor(Vec3 axis, Vec3 normal) {
        if (axis.lengthSqr() < TOLERANCE) {
            return null;
        }
        Vec3 direction = axis.normalize();
        Vec3 up = normal.lengthSqr() < TOLERANCE ? new Vec3(0, 1, 0) : normal.normalize();
        TrackShape parallelOnly = null;
        for (TrackShape shape : TrackShape.values()) {
            List<Vec3> axes = shape.getAxes();
            if (shape == TrackShape.NONE || shape.isPortal() || axes.size() != 1) {
                continue;
            }
            Vec3 candidate = axes.getFirst().normalize();
            if (Math.abs(Math.abs(candidate.dot(direction)) - 1) > TOLERANCE) {
                continue;
            }
            if (shape.getNormal().distanceTo(up) < TOLERANCE) {
                return shape;
            }
            if (parallelOnly == null) {
                parallelOnly = shape;
            }
        }
        return parallelOnly;
    }
}
