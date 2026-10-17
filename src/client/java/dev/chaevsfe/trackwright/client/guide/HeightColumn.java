package dev.chaevsfe.trackwright.client.guide;

import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.client.AllSpecialTextures;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackBlockItem;
import com.zurrtum.create.content.trains.track.TrackPlacement;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.zurrtum.create.infrastructure.component.ConnectingFrom;
import dev.chaevsfe.trackwright.client.config.ClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record HeightColumn(BlockPos aimed, List<Entry> entries, @Nullable Integer nearest, int lowest, int highest) {
    public static final int SPAN = 6;

    private static final String OUTLINE = "trackwright_height_";
    private static final int VALID_COLOR = 0x95CD41;
    private static final int INVALID_COLOR = 0xEA5C2B;
    private static final int NEAREST_COLOR = 0xE4FF9E;
    private static final double MARKER_HEIGHT = 1 / 16.0;
    private static final Set<String> HEIGHT_MESSAGES = Set.of(
        "track.too_steep",
        "track.slope_turn",
        "track.ascending_s_curve",
        "track.leave_slope_ascending",
        "track.leave_slope_descending",
        "track.opposing_slopes"
    );

    public record Entry(BlockPos pos, boolean connects) {
    }

    public static @Nullable HeightColumn compute(Level level, Player player, BlockPos target, ItemStack stack, boolean maximise, PlacementInfo info) {
        if (!ClientConfig.get().showHeightHints() || !(stack.getItem() instanceof TrackBlockItem item)) {
            return null;
        }
        ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null || GuideLines.geometryFinal(info) || info.message != null && GuideLines.beforeGeometry(info.message)) {
            return null;
        }
        boolean heightMessage = info.message != null && HEIGHT_MESSAGES.contains(info.message);
        if (from.pos().getY() == target.getY() && !heightMessage) {
            return null;
        }
        InteractionHand hand = player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        int lowest = Math.max(target.getY() - SPAN, level.getMinY());
        int highest = Math.min(target.getY() + SPAN, level.getMaxY());
        List<Entry> entries = new ArrayList<>();
        PlacementCache saved = PlacementCache.save();
        try {
            for (int y = lowest; y <= highest; y++) {
                if (y == target.getY()) {
                    continue;
                }
                BlockPos cell = target.atY(y);
                if (!level.isLoaded(cell)) {
                    continue;
                }
                BlockState here = level.getBlockState(cell);
                BlockState state;
                if (here.getBlock() instanceof ITrackBlock) {
                    state = here;
                } else if (here.canBeReplaced()) {
                    state = item.getPlacementState(new UseOnContext(player, hand, new BlockHitResult(Vec3.atCenterOf(cell), Direction.UP, cell, false)));
                } else {
                    continue;
                }
                if (state == null || !(state.getBlock() instanceof ITrackBlock)) {
                    continue;
                }
                PlacementInfo result = TrackPlacement.tryConnect(level, player, cell, state, stack, false, maximise);
                entries.add(new Entry(cell.immutable(), GuideLines.geometryFinal(result)));
            }
        } finally {
            saved.restore();
        }
        Integer nearest = null;
        for (Entry entry : entries) {
            if (!entry.connects()) {
                continue;
            }
            int distance = Math.abs(entry.pos().getY() - target.getY());
            if (nearest == null || distance < Math.abs(nearest - target.getY())) {
                nearest = entry.pos().getY();
            }
        }
        return new HeightColumn(target.immutable(), List.copyOf(entries), nearest, lowest, highest);
    }

    public void show() {
        Integer best = nearest;
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            boolean isNearest = best != null && entry.pos().getY() == best;
            double inset = isNearest ? 0.1 : entry.connects() ? 0.25 : 0.375;
            BlockPos pos = entry.pos();
            AABB box = new AABB(pos.getX() + inset, pos.getY(), pos.getZ() + inset, pos.getX() + 1 - inset, pos.getY() + MARKER_HEIGHT, pos.getZ() + 1 - inset);
            Outliner.getInstance().showAABB(OUTLINE + i, box)
                .colored(isNearest ? NEAREST_COLOR : entry.connects() ? VALID_COLOR : INVALID_COLOR)
                .lineWidth(1 / 32f)
                .disableLineNormals()
                .withFaceTexture(AllSpecialTextures.THIN_CHECKERED);
        }
    }
}
