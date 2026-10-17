package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.zurrtum.create.content.trains.track.TrackShape;
import dev.chaevsfe.trackwright.config.BoreMode;
import dev.chaevsfe.trackwright.config.Sides;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import dev.chaevsfe.trackwright.undo.UndoMessages;
import dev.chaevsfe.trackwright.undo.UndoRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

public final class PlacementRules {
    private static final int STACK = 64;

    private PlacementRules() {
    }

    public record Straight(boolean bridge, boolean unseen, List<BlockPos> blocked, @Nullable BorePlan bore, BoreAccess access) {
        static final Straight KEEP = new Straight(false, false, List.of(), null, BoreAccess.NONE);
    }

    public static Straight decideStraight(Level level, Player player, BlockPos pos1, BlockState end2, Vec3 axis1, Vec3 axis2, int cells) {
        TrackwrightSettings settings = Sides.forLevel(level);
        if (settings == null || cells < 3) {
            return Straight.KEEP;
        }
        boolean tool = BlockMiner.isTool(player.getOffhandItem());
        boolean boreAllowed = settings.boreThroughBlocks().allows(player);
        boolean bore = tool && boreAllowed;
        boolean bridges = settings.straightThroughBlocks() && end2.hasProperty(TrackBlock.HAS_BE);
        if (!boreAllowed && !bridges) {
            return Straight.KEEP;
        }
        StraightConnections.Line line = StraightConnections.line(pos1, axis1);
        if (line == null || StraightConnections.line(pos1, axis2) == null) {
            return Straight.KEEP;
        }
        List<BlockPos> blocked = new ArrayList<>();
        boolean trackInside = false;
        boolean unseen = false;
        BlockPos unloaded = null;
        for (int i = 1; i < cells - 1; i++) {
            BlockPos pos = line.at(i);
            if (level.isClientSide()) {
                if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
                    unseen = true;
                    continue;
                }
            } else if (bore && !level.isLoaded(pos)) {
                if (unloaded == null) {
                    unloaded = pos;
                }
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ITrackBlock) {
                trackInside = true;
            } else if (!state.canBeReplaced() && !state.is(BlockTags.FLOWERS)) {
                blocked.add(pos);
            }
        }
        if (blocked.isEmpty()) {
            return new Straight(false, unseen, List.of(), null, BoreAccess.NONE);
        }
        BoreAccess access = tool
            ? boreAllowed ? BoreAccess.ALLOWED : settings.boreThroughBlocks() == BoreMode.CREATIVE_ONLY ? BoreAccess.CREATIVE_ONLY : BoreAccess.OFF
            : boreAllowed ? BoreAccess.NO_TOOL : BoreAccess.NONE;
        if (bore) {
            return new Straight(false, unseen, blocked, BorePlan.of(level, player, line, cells, new HashSet<>(blocked), unloaded, unseen, settings), access);
        }
        if (bridges && !trackInside) {
            return new Straight(true, unseen, blocked, null, access);
        }
        return new Straight(false, unseen, List.of(), null, access);
    }

    public static @Nullable String refusal(Level level, Player player, PlacementInfo info, BlockPos pos1, BlockPos pos2, Vec3 axis1, Vec3 axis2) {
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
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
        BlockPos keepA = null;
        BlockPos keepB = null;
        if (!curve && line1 != null && cells1 > 0 && BridgeConversion.existing(level, pos1, pos2) != null) {
            keepA = pos1;
            keepB = pos2;
            extension.trackwright$setConverts(true);
        }
        if (line1 != null && cells1 > 0 && StraightConnections.overlapsStraight(level, line1, 0, cells1 - 1, keepA, keepB)) {
            return Messages.OVERLAPS_STRAIGHT;
        }
        int cells2 = curve ? info.end2Extent + 1 : info.end2Extent;
        StraightConnections.Line line2 = StraightConnections.line(pos2, axis2);
        if (line2 != null && cells2 > 0 && StraightConnections.overlapsStraight(level, line2, 0, cells2 - 1, null, null)) {
            return Messages.OVERLAPS_STRAIGHT;
        }
        if (extension.trackwright$converts()) {
            String conversion = BridgeConversion.refusal(level, pos1, pos2);
            if (conversion != null) {
                return conversion;
            }
        }
        BorePlan bore = extension.trackwright$bore();
        if (bore == null) {
            return null;
        }
        if (bore.problem() != null) {
            return bore.problem().refusal().message();
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            BlockMiner.Problem problem = BlockMiner.checkPermission(serverLevel, serverPlayer, bore.cells());
            if (problem != null) {
                extension.trackwright$setBore(bore.withProblem(problem));
                return problem.refusal().message();
            }
        }
        return null;
    }

    public static PlacementInfo placeTracks(Level level, Player player, PlacementInfo info, BlockPos pos1, BlockPos pos2, boolean simulate,
                                            Supplier<PlacementInfo> original) {
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        BorePlan bore = extension.trackwright$bore();
        if (simulate) {
            PlacementInfo result = original.get();
            if (bore != null) {
                result.requiredTracks += bore.trackCells().size();
            }
            return result;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (bore != null && !bore.cells().isEmpty()) {
                UndoRecorder.beforeMining(serverLevel, serverPlayer, bore.cells());
                BlockMiner.Outcome outcome = BlockMiner.mine(serverLevel, serverPlayer, bore.cells());
                UndoRecorder.afterMining(serverLevel, serverPlayer);
                if (outcome.stoppedAt() != null) {
                    refundTracks(serverPlayer, info);
                    info.valid = false;
                    return info.withMessage(Messages.BORE_STOPPED).tooJumbly();
                }
            }
            if (extension.trackwright$converts()) {
                UndoRecorder.notUndoable(serverLevel, UndoMessages.CONVERTED);
                BridgeConversion.remove(serverLevel, serverPlayer, pos1, pos2);
            }
        }
        return original.get();
    }

    private static void refundTracks(ServerPlayer player, PlacementInfo info) {
        if (player.isCreative()) {
            return;
        }
        int tracks = info.requiredTracks;
        while (tracks > 0) {
            int count = Math.min(STACK, tracks);
            player.getInventory().placeItemBackInInventory(new ItemStack(info.trackMaterial.getBlock(), count));
            tracks -= count;
        }
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
