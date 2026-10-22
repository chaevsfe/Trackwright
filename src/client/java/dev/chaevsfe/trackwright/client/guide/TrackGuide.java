package dev.chaevsfe.trackwright.client.guide;

import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.AllItemTags;
import com.zurrtum.create.client.AllSpecialTextures;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import com.zurrtum.create.content.trains.track.TrackPlacement;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.zurrtum.create.infrastructure.component.ConnectingFrom;
import dev.chaevsfe.trackwright.client.mixin.TrackPlacementClientAccessor;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import dev.chaevsfe.trackwright.track.BorePlan;
import dev.chaevsfe.trackwright.track.Gap;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
        TrackPlacementClientAccessor.trackwright$setHintPos(null);
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
        HeightColumn column = extras.column();
        if (column != null) {
            column.show();
        }
    }

    private static void showThroughBlocks(PlacementInfo info) {
        if (!info.valid) {
            return;
        }
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
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
        if (!bore.headroomCells().isEmpty()) {
            Outliner.getInstance().showCluster(BORE_HEADROOM_OUTLINE, bore.headroomCells())
                .colored(BORE_HEADROOM_COLOR)
                .disableLineNormals()
                .lineWidth(1 / 64f)
                .withFaceTexture(AllSpecialTextures.THIN_CHECKERED);
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
        if (current != null && current.from.equals(from) && ItemStack.isSameItemSameComponents(current.stack, stack)) {
            if (walkCloser <= 0) {
                return current.lines;
            }
            List<Component> lines = new ArrayList<>(current.lines);
            lines.add(GuideLines.outOfReach(walkCloser));
            return lines;
        }
        return GuideLines.waiting(from, walkCloser);
    }

    private static @Nullable ItemStack heldTrack(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.is(AllItemTags.TRACKS)) {
            return stack;
        }
        stack = player.getOffhandItem();
        return stack.is(AllItemTags.TRACKS) ? stack : null;
    }
}
