package dev.chaevsfe.trackwright.track;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BridgePaving {
    private BridgePaving() {
    }

    public static int itemsNeeded(PlacementInfo info, Block block, int curveRate) {
        if (!((PlacementInfoExtension) info).trackwright$bridged()) {
            return curveRate;
        }
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(SlabBlock.TYPE) || block instanceof WallBlock || AllBlocks.METAL_GIRDER.has(state)) {
            return curveRate;
        }
        return curveRate / 2;
    }
}
