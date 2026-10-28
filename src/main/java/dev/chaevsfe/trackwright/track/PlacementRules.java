package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.content.kinetics.deployer.DeployerPlayer;
import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackBlock;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.zurrtum.create.content.trains.track.TrackShape;
import dev.chaevsfe.trackwright.config.BoreMode;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.Sides;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import dev.chaevsfe.trackwright.undo.UndoMessages;
import dev.chaevsfe.trackwright.undo.UndoRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class PlacementRules {
    private static final int STACK = 64;

    private PlacementRules() {
    }

    public record Straight(boolean bridge, boolean unseen, List<BlockPos> blocked, @Nullable BorePlan bore, BoreAccess access) {
        static final Straight KEEP = new Straight(false, false, List.of(), null, BoreAccess.NONE);
    }

    private static final class PathScan {
        private final Level level;
        private final boolean skipUnloaded;
        private final Map<BlockPos, BlockState> blocked = new LinkedHashMap<>();
        private boolean unseen;
        private boolean trackInside;
        private @Nullable BlockPos unloaded;

        private PathScan(Level level, boolean skipUnloaded) {
            this.level = level;
            this.skipUnloaded = skipUnloaded;
        }

        private void visit(Collection<BlockPos> cells) {
            for (BlockPos pos : cells) {
                if (!loaded(level, pos)) {
                    if (level.isClientSide()) {
                        unseen = true;
                        continue;
                    }
                    if (unloaded == null) {
                        unloaded = pos;
                    }
                    if (skipUnloaded) {
                        continue;
                    }
                }
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof ITrackBlock) {
                    trackInside = true;
                } else if (PathCells.blocks(state)) {
                    blocked.put(pos, state);
                }
            }
        }
    }

    private static boolean loaded(Level level, BlockPos pos) {
        return level.isClientSide()
            ? level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))
            : level.isLoaded(pos);
    }

    private static boolean holdsBoreTool(Player player) {
        return !(player instanceof DeployerPlayer) && BlockMiner.isTool(player.getOffhandItem());
    }

    private static BoreAccess access(TrackwrightSettings settings, boolean tool, boolean boreAllowed) {
        return tool
            ? boreAllowed ? BoreAccess.ALLOWED : settings.boreThroughBlocks() == BoreMode.CREATIVE_ONLY ? BoreAccess.CREATIVE_ONLY : BoreAccess.OFF
            : boreAllowed ? BoreAccess.NO_TOOL : BoreAccess.NONE;
    }

    public static Straight decideStraight(Level level, Player player, PlacementInfo info, BlockPos pos1, BlockState end2, Vec3 axis1, Vec3 axis2,
                                          int cells) {
        TrackwrightSettings settings = Sides.forLevel(level);
        if (settings == null || cells < 3) {
            return Straight.KEEP;
        }
        boolean tool = holdsBoreTool(player);
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
        List<BlockPos> path = new ArrayList<>();
        for (int i = 1; i < cells - 1; i++) {
            path.add(line.at(i));
        }
        PathScan scan = new PathScan(level, bore);
        scan.visit(path);
        if (scan.blocked.isEmpty()) {
            return new Straight(false, scan.unseen, List.of(), null, BoreAccess.NONE);
        }
        List<BlockPos> blocked = new ArrayList<>(scan.blocked.keySet());
        BoreAccess access = access(settings, tool, boreAllowed);
        if (bore) {
            BlockPos pos2 = line.at(cells - 1);
            BoreProfile profile = BoreProfiles.resolve(level, player, info.trackMaterial, settings);
            if (BridgeConversion.existing(level, pos1, pos2) != null) {
                profile = new BoreProfile(0, 0, profile.up());
            }
            BoreBand.Line seeds = new BoreBand.Builder().cell(pos1).cells(path).cell(pos2).centre(pos1).centre(pos2).build();
            BorePlan plan = BorePlan.of(level, player, path, new HashSet<>(blocked), Set.of(), seeds, profile, scan.unloaded, scan.unseen, settings);
            return new Straight(false, scan.unseen, blocked, plan, access);
        }
        if (bridges && !scan.trackInside) {
            return new Straight(true, scan.unseen, blocked, null, access);
        }
        return new Straight(false, scan.unseen, List.of(), null, access);
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
        if (bore == null && !extension.trackwright$bridged() && (curve || axis1.y != 0)) {
            String gap = decidePath(level, player, info, pos1, pos2, axis1, axis2);
            if (gap != null) {
                return gap;
            }
            bore = extension.trackwright$bore();
        }
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

    private static @Nullable String decidePath(Level level, Player player, PlacementInfo info, BlockPos pos1, BlockPos pos2, Vec3 axis1, Vec3 axis2) {
        TrackwrightSettings settings = Sides.forLevel(level);
        if (settings == null) {
            return null;
        }
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        boolean tool = holdsBoreTool(player);
        boolean boreAllowed = settings.boreThroughBlocks().allows(player);
        boolean bore = tool && boreAllowed;
        PathCells.Ends ends = PathCells.ends(info, pos1, pos2, axis1, axis2);
        PathScan endScan = new PathScan(level, bore);
        endScan.visit(ends.first());
        endScan.visit(ends.second());
        if (endScan.unseen) {
            extension.trackwright$setGuessed(true);
        }
        if (!bore) {
            if (endScan.blocked.isEmpty()) {
                return null;
            }
            BoreAccess access = access(settings, tool, boreAllowed);
            if (!settings.refuseBlockedTrackEnds()) {
                if (tool) {
                    extension.trackwright$setBoreAccess(access);
                }
                return null;
            }
            Gap gap = Gap.of(endScan.blocked, ends.starts());
            if (access == BoreAccess.NO_TOOL && !boreCouldClear(level, player, info, ends, pos1, pos2, endScan, settings)) {
                access = BoreAccess.NONE;
            }
            extension.trackwright$setBoreAccess(access);
            extension.trackwright$setGap(gap);
            return gap.start() ? Messages.GAP_START : Messages.GAP;
        }
        Route route = route(level, info, ends, pos1, pos2, endScan);
        if (route.unseen()) {
            extension.trackwright$setGuessed(true);
        }
        if (endScan.blocked.isEmpty() && route.bodyClear() && endScan.unloaded == null) {
            return null;
        }
        extension.trackwright$setBoreAccess(BoreAccess.ALLOWED);
        BoreProfile profile = BoreProfiles.resolve(level, player, info.trackMaterial, settings);
        BorePlan plan = BorePlan.of(level, player, route.path(), route.blocked(), route.bodyCells(), route.seeds(), profile, route.unloaded(),
            route.unseen(), settings);
        extension.trackwright$setBore(plan);
        if (plan.guessed()) {
            extension.trackwright$setGuessed(true);
        }
        return null;
    }

    private record Route(List<BlockPos> path, Set<BlockPos> blocked, Set<BlockPos> bodyCells, BoreBand.Line seeds, @Nullable BlockPos unloaded,
                         boolean unseen, boolean bodyClear) {
    }

    private static void curvePoints(BoreBand.Builder builder, BezierConnection curve, BlockPos pos1) {
        Vec3 start = curve.starts.getFirst();
        Vec3 end = curve.starts.getSecond();
        double handle = curve.getHandleLength();
        Vec3 control1 = start.add(curve.axes.getFirst().scale(handle));
        Vec3 control2 = end.add(curve.axes.getSecond().scale(handle));
        Vec3 centre = Vec3.atCenterOf(pos1);
        boolean reversed = start.distanceToSqr(centre) > end.distanceToSqr(centre);
        int samples = Math.max(8, (int) Math.ceil(curve.getLength() * 4));
        for (int i = 0; i <= samples; i++) {
            float t = i / (float) samples;
            builder.point(VecHelper.bezier(start, end, control1, control2, reversed ? 1 - t : t));
        }
    }

    private static Route route(Level level, PlacementInfo info, PathCells.Ends ends, BlockPos pos1, BlockPos pos2, PathScan endScan) {
        PathCells.Body body = PathCells.Body.NONE;
        if (info.curve != null) {
            Set<BlockPos> excluded = new HashSet<>(ends.first());
            excluded.addAll(ends.second());
            excluded.add(pos1);
            excluded.add(pos2);
            body = PathCells.body(info.curve, excluded);
        }
        PathScan bodyScan = new PathScan(level, true);
        bodyScan.visit(body.cells());
        List<BlockPos> beds = new ArrayList<>();
        for (BlockPos cell : body.cells()) {
            BlockPos bed = body.beds().get(cell);
            if (bed != null && bodyScan.blocked.containsKey(cell)) {
                beds.add(bed);
            }
        }
        bodyScan.visit(beds);
        List<BlockPos> path = new ArrayList<>(ends.first());
        path.addAll(body.cells());
        path.addAll(beds);
        path.addAll(ends.second().reversed());
        Set<BlockPos> blocked = new HashSet<>(endScan.blocked.keySet());
        blocked.addAll(bodyScan.blocked.keySet());
        Set<BlockPos> bodyCells = new HashSet<>(body.cells());
        bodyCells.addAll(beds);
        BlockPos unloaded = endScan.unloaded != null ? endScan.unloaded : bodyScan.unloaded;
        BoreBand.Builder line = new BoreBand.Builder().cell(pos1).cells(ends.first()).cells(body.cells()).cells(ends.second().reversed()).cell(pos2);
        line.centre(pos1);
        BezierConnection curve = info.curve;
        if (curve != null) {
            curvePoints(line, curve, pos1);
        }
        line.centre(pos2);
        BoreBand.Line seeds = line.build();
        return new Route(path, blocked, bodyCells, seeds, unloaded, endScan.unseen || bodyScan.unseen, bodyScan.blocked.isEmpty());
    }

    private static boolean boreCouldClear(Level level, Player player, PlacementInfo info, PathCells.Ends ends, BlockPos pos1, BlockPos pos2,
                                          PathScan endScan, TrackwrightSettings settings) {
        if (endScan.unseen || endScan.unloaded != null) {
            return false;
        }
        Route route = route(level, info, ends, pos1, pos2, endScan);
        if (route.unseen() || route.unloaded() != null) {
            return false;
        }
        BoreProfile profile = BoreProfiles.resolve(level, player, info.trackMaterial, settings);
        BorePlan plan = BorePlan.of(level, ItemStack.EMPTY, true, route.path(), route.blocked(), route.bodyCells(), route.seeds(), profile, null, false,
            settings);
        if (plan.guessed() || plan.problem() != null) {
            return false;
        }
        Set<BlockPos> mined = new HashSet<>(plan.cells());
        for (BlockPos pos : plan.cells()) {
            if (!mined.contains(pos.above()) && !level.isLoaded(pos.above())) {
                return false;
            }
            if (!settings.boreLavaCheck()) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!mined.contains(next) && !level.isLoaded(next)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static PlacementInfo placeTracks(Level level, Player player, PlacementInfo info, BlockPos pos1, BlockPos pos2, boolean simulate,
                                            Supplier<PlacementInfo> original) {
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        BorePlan bore = extension.trackwright$bore();
        if (simulate) {
            PlacementInfo result = original.get();
            if (bore != null && result.valid) {
                result.requiredTracks += bore.trackCells().size();
            }
            return result;
        }
        if (!info.valid) {
            return original.get();
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
