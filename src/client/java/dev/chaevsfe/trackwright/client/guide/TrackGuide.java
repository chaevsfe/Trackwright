package dev.chaevsfe.trackwright.client.guide;

import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.AllItemTags;
import com.zurrtum.create.client.AllSpecialTextures;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.zurrtum.create.infrastructure.component.ConnectingFrom;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class TrackGuide {
    private static final String THROUGH_BLOCKS_OUTLINE = "trackwright_through_blocks";
    private static final int THROUGH_BLOCKS_COLOR = 0x5AC8FA;

    private static @Nullable Planned planned;
    private static @Nullable Planned lastPlanned;

    private TrackGuide() {
    }

    private record Planned(PlacementInfo info, ConnectingFrom from, BlockPos target, ItemStack stack, List<Component> lines) {
    }

    public static void forget() {
        planned = null;
    }

    public static void reset() {
        planned = null;
        lastPlanned = null;
    }

    public static void capture(BlockPos target, ItemStack stack, PlacementInfo info) {
        ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null) {
            planned = null;
            return;
        }
        Planned previous = lastPlanned;
        if (previous != null && previous.info == info && previous.from.equals(from) && previous.target.equals(target)) {
            planned = new Planned(info, from, previous.target, stack, previous.lines);
        } else {
            planned = new Planned(info, from, target.immutable(), stack, GuideLines.planned(from, target, info));
        }
        lastPlanned = planned;
        showThroughBlocks(info);
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

    public static List<Component> linesFor(Player player) {
        ItemStack stack = heldTrack(player);
        if (stack == null) {
            return List.of();
        }
        ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null) {
            return List.of();
        }
        Planned current = planned;
        if (current != null && current.from.equals(from) && ItemStack.isSameItemSameComponents(current.stack, stack)) {
            return current.lines;
        }
        return GuideLines.waiting(from);
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
