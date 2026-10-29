package dev.chaevsfe.trackwright.client.guide;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.content.trains.track.TrackPlacement.ConnectingFrom;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.simibubi.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.client.mixin.TrackPlacementAccessor;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import dev.chaevsfe.trackwright.track.BoreBand;
import dev.chaevsfe.trackwright.track.BorePlan;
import dev.chaevsfe.trackwright.track.Gap;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class TrackGuide {
    private static final String THROUGH_BLOCKS_OUTLINE = "trackwright_through_blocks";
    private static final int THROUGH_BLOCKS_COLOR = 0x5AC8FA;
    private static final String BORE_OUTLINE = "trackwright_bore";
    private static final String BORE_HEADROOM_OUTLINE = "trackwright_bore_headroom";
    private static final String BORE_PROBLEM_OUTLINE = "trackwright_bore_problem";
    private static final String GAP_OUTLINE = "trackwright_gaps";
    private static final int BORE_COLOR = 0xF0A030;
    private static final int BORE_HEADROOM_COLOR = 0xF6D28C;
    private static final int BORE_PROBLEM_COLOR = 0xEA5C2B;
    private static final String FRAME_OUTLINE = "trackwright_bore_frame_";
    private static final int FRAME_COLOR = 0xF0A030;
    private static final String[] FRAME_EDGES = {"bottom", "right", "top", "left"};

    private static @Nullable Planned planned;
    private static @Nullable Planned lastPlanned;
    private static ItemStack lastOffhand = ItemStack.EMPTY;

    private TrackGuide() {
    }

    private record Planned(PlacementInfo info, ConnectingFrom from, BlockPos target, ItemStack stack, PlanExtras extras, List<Component> lines) {
    }

    public static void beginTick(Minecraft mc) {
        planned = null;
        ItemStack offhand = mc.player == null ? ItemStack.EMPTY : mc.player.getOffhandItem();
        if (!ItemStack.matches(offhand, lastOffhand)) {
            lastOffhand = offhand.copy();
            forgetPlan();
        }
    }

    public static void forgetPlan() {
        TrackPlacement.cached = null;
        PlanExtras.reset();
        TrackPlacementAccessor.trackwright$setHintPos(null);
    }

    public static void reset() {
        planned = null;
        lastPlanned = null;
        lastOffhand = ItemStack.EMPTY;
        PlanExtras.reset();
        AimHelp.reset();
    }

    public static void capture(Level level, Player player, BlockPos target, BlockState state, ItemStack stack, boolean maximise, PlacementInfo info) {
        ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null) {
            planned = null;
            return;
        }
        PlanExtras extras = PlanExtras.of(level, player, target, state, stack, maximise, info);
        Planned previous = lastPlanned;
        if (previous != null && previous.info == info && previous.extras == extras && previous.from.equals(from) && previous.target.equals(target)) {
            planned = new Planned(info, from, previous.target, stack, extras, previous.lines);
        } else {
            planned = new Planned(info, from, target.immutable(), stack, extras, GuideLines.planned(from, target, info, extras, player.getOffhandItem()));
        }
        lastPlanned = planned;
        showThroughBlocks(info);
        showBore(info);
        showGaps(info);
        showFrames(player, from, target, info);
        HeightColumn column = extras.column();
        if (column != null) {
            column.show();
        }
    }

    private static void showThroughBlocks(PlacementInfo info) {
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        if (!extension.trackwright$valid()) {
            return;
        }
        if (!extension.trackwright$bridged() || extension.trackwright$bridgedCells().isEmpty()) {
            return;
        }
        Outliner.getInstance().showCluster(THROUGH_BLOCKS_OUTLINE, extension.trackwright$bridgedCells())
            .colored(THROUGH_BLOCKS_COLOR)
            .disableLineNormals()
            .lineWidth(1 / 32f)
            .withFaceTexture(AllSpecialTextures.CHECKERED);
    }

    private static void showBore(PlacementInfo info) {
        BorePlan bore = ((PlacementInfoExtension) info).trackwright$bore();
        if (bore == null) {
            return;
        }
        BlockMiner.Problem problem = bore.problem();
        if (problem != null && problem.refusal() != BlockMiner.Refusal.NOT_LOADED) {
            Outliner.getInstance().showAABB(BORE_PROBLEM_OUTLINE, new AABB(problem.pos()).inflate(1 / 64.0))
                .colored(BORE_PROBLEM_COLOR)
                .disableLineNormals()
                .lineWidth(1 / 16f)
                .withFaceTexture(AllSpecialTextures.CHECKERED);
        }
        List<BlockPos> path = bore.pathCells();
        if (!path.isEmpty()) {
            Outliner.getInstance().showCluster(BORE_OUTLINE, path)
                .colored(BORE_COLOR)
                .disableLineNormals()
                .lineWidth(1 / 32f)
                .withFaceTexture(AllSpecialTextures.CHECKERED);
        }
        if (!bore.bandCells().isEmpty()) {
            Outliner.getInstance().showCluster(BORE_HEADROOM_OUTLINE, bore.bandCells())
                .colored(BORE_HEADROOM_COLOR)
                .disableLineNormals()
                .lineWidth(1 / 64f)
                .withFaceTexture(AllSpecialTextures.THIN_CHECKERED);
        }
    }

    public static @Nullable PlacementInfo plannedFrom(ConnectingFrom from) {
        Planned current = lastPlanned;
        return current != null && current.from.equals(from) ? current.info : null;
    }

    private static void showFrames(Player player, ConnectingFrom from, BlockPos target, PlacementInfo info) {
        if (!BoreShaping.boring(player)) {
            return;
        }
        BoreProfile profile = BoreShaping.current(player);
        if (profile == null) {
            return;
        }
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        Vec3 first = extension.trackwright$axis1();
        if (first == null) {
            first = from.axis();
            Vec3 toward = Vec3.atCenterOf(target).subtract(Vec3.atCenterOf(from.pos()));
            if (first.x * toward.x + first.z * toward.z < 0) {
                first = first.scale(-1);
            }
        }
        Vec3 second = extension.trackwright$axis2();
        Vec3 last = second != null ? second.scale(-1) : first;
        showFrame("first", from.pos(), first, profile);
        if (!target.equals(from.pos())) {
            showFrame("aimed", target, last, profile);
        }
    }

    private static void showFrame(String name, BlockPos cell, Vec3 direction, BoreProfile profile) {
        BoreBand.Seed seed = BoreBand.Seed.of(cell, direction);
        if (seed == null) {
            return;
        }
        double cx = cell.getX() + 0.5;
        double cz = cell.getZ() + 0.5;
        double left = profile.left() + 0.5;
        double right = profile.right() + 0.5;
        double height = profile.height();
        Vec3 a = new Vec3(cx - seed.rightX() * left, cell.getY(), cz - seed.rightZ() * left);
        Vec3 b = new Vec3(cx + seed.rightX() * right, cell.getY(), cz + seed.rightZ() * right);
        Vec3 c = b.add(0, height, 0);
        Vec3 d = a.add(0, height, 0);
        Vec3[][] edges = {{a, b}, {b, c}, {c, d}, {d, a}};
        for (int i = 0; i < edges.length; i++) {
            Outliner.getInstance().showLine(FRAME_OUTLINE + name + "_" + FRAME_EDGES[i], edges[i][0], edges[i][1])
                .colored(FRAME_COLOR)
                .disableLineNormals()
                .lineWidth(1 / 16f);
        }
    }

    private static void showGaps(PlacementInfo info) {
        Gap gap = ((PlacementInfoExtension) info).trackwright$gap();
        if (gap == null || gap.cells().isEmpty()) {
            return;
        }
        Outliner.getInstance().showCluster(GAP_OUTLINE, gap.cells())
            .colored(BORE_PROBLEM_COLOR)
            .disableLineNormals()
            .lineWidth(1 / 32f)
            .withFaceTexture(AllSpecialTextures.CHECKERED);
    }

    public static List<Component> linesFor(Player player) {
        ItemStack stack = heldTrack(player);
        if (stack == null) {
            return List.of();
        }
        ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null) {
            return UndoKey.live() && UndoKey.bound() ? GuideLines.undoHint() : List.of();
        }
        int walkCloser = AimHelp.walkCloser();
        Planned current = planned;
        List<Component> shaping = BoreShaping.guideLines(player);
        if (current != null && current.from.equals(from) && ItemStack.isSameItemSameComponents(current.stack, stack)) {
            if (walkCloser <= 0 && shaping.isEmpty()) {
                return current.lines;
            }
            List<Component> lines = new ArrayList<>(current.lines);
            if (walkCloser > 0) {
                lines.add(GuideLines.outOfReach(walkCloser));
            }
            lines.addAll(shaping);
            return lines;
        }
        List<Component> lines = new ArrayList<>(GuideLines.waiting(from, walkCloser));
        lines.addAll(shaping);
        return lines;
    }

    private static @Nullable ItemStack heldTrack(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (AllBlockTags.TRACKS.matches(stack)) {
            return stack;
        }
        stack = player.getOffhandItem();
        return AllBlockTags.TRACKS.matches(stack) ? stack : null;
    }
}
