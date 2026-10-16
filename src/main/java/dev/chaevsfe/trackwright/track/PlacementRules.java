package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.zurrtum.create.content.trains.track.TrackShape;
import dev.chaevsfe.trackwright.config.Sides;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PlacementRules {
    private PlacementRules() {
    }

    public enum StraightDecision {
        KEEP,
        BRIDGE,
        OCCUPIED
    }

    public record Straight(StraightDecision decision, List<BlockPos> blocked) {
        static final Straight KEEP = new Straight(StraightDecision.KEEP, List.of());
    }

    public static Straight decideStraight(Level level, BlockPos pos1, BlockState end2, Vec3 axis1, Vec3 axis2, int cells) {
        TrackwrightSettings settings = Sides.forLevel(level);
        if (settings == null || !settings.straightThroughBlocks() || cells < 3 || !end2.hasProperty(TrackBlock.HAS_BE)) {
            return Straight.KEEP;
        }
        StraightConnections.Line line = StraightConnections.line(pos1, axis1);
        if (line == null || StraightConnections.line(pos1, axis2) == null) {
            return Straight.KEEP;
        }
        List<BlockPos> blocked = new ArrayList<>();
        boolean trackInside = false;
        for (int i = 1; i < cells - 1; i++) {
            BlockPos pos = line.at(i);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ITrackBlock) {
                trackInside = true;
            } else if (!state.canBeReplaced() && !state.is(BlockTags.FLOWERS)) {
                blocked.add(pos);
            }
        }
        if (blocked.isEmpty()) {
            return Straight.KEEP;
        }
        return new Straight(trackInside ? StraightDecision.OCCUPIED : StraightDecision.BRIDGE, blocked);
    }

    public static @Nullable String refusal(Level level, PlacementInfo info, BlockPos pos1, BlockPos pos2, Vec3 axis1, Vec3 axis2) {
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        String decided = extension.trackwright$refusal();
        if (decided != null) {
            return decided;
        }
        if (Sides.forLevel(level) == null) {
            return null;
        }
        StraightConnections.Line line1 = StraightConnections.line(pos1, axis1);
        if (extension.trackwright$bridged() && line1 != null) {
            Integer end = line1.stepOf(pos2);
            if (end != null && end > 1 && StraightConnections.overlapsStraight(level, line1, 1, end - 1, pos1, pos2)) {
                return Messages.OVERLAPS_STRAIGHT;
            }
        }
        boolean curve = info.curve != null;
        int cells1 = curve ? info.end1Extent + 1 : info.end1Extent;
        if (line1 != null && cells1 > 0 && StraightConnections.overlapsStraight(level, line1, 0, cells1 - 1, null, null)) {
            return Messages.OVERLAPS_STRAIGHT;
        }
        int cells2 = curve ? info.end2Extent + 1 : info.end2Extent;
        StraightConnections.Line line2 = StraightConnections.line(pos2, axis2);
        if (line2 != null && cells2 > 0 && StraightConnections.overlapsStraight(level, line2, 0, cells2 - 1, null, null)) {
            return Messages.OVERLAPS_STRAIGHT;
        }
        return null;
    }

    public static @Nullable BlockState guardSinglePlacement(@Nullable BlockState state, BlockPlaceContext context) {
        if (state == null || !state.hasProperty(TrackBlock.SHAPE)) {
            return state;
        }
        Level level = context.getLevel();
        if (Sides.forLevel(level) == null) {
            return state;
        }
        TrackShape shape = state.getValue(TrackBlock.SHAPE);
        List<Vec3> axes = shape.getAxes();
        if (axes.size() != 1) {
            return state;
        }
        BlockPos pos = context.getClickedPos();
        StraightConnections.Line line = StraightConnections.line(pos, axes.getFirst());
        if (line == null || !StraightConnections.overlapsStraight(level, line, 0, 0, null, null)) {
            return state;
        }
        if (context.getPlayer() instanceof ServerPlayer player) {
            player.sendOverlayMessage(Messages.overlay(Messages.OVERLAPS_STRAIGHT));
        }
        return null;
    }
}
