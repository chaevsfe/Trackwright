package dev.chaevsfe.trackwright.track;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class BridgePermission {
    private BridgePermission() {
    }

    public static boolean allowed(ServerLevel level, Player player, BezierConnection curve) {
        BlockPos origin = curve.bePositions.getFirst();
        List<BlockPos> cells = new ArrayList<>();
        for (Map.Entry<Pair<Integer, Integer>, Double> column : LoadedColumns.filter(level, origin, curve.rasterise()).entrySet()) {
            cells.add(new BlockPos(column.getKey().getFirst(), Mth.floor(column.getValue()), column.getKey().getSecond()).offset(origin).above());
        }
        cells.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        for (BlockPos pos : cells) {
            BlockState state = level.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            if (AllBlocks.FAKE_TRACK.has(state) || !state.canBeReplaced() || !fluid.isEmpty() && !fluid.isSourceOfType(Fluids.WATER)) {
                continue;
            }
            BlockState planned = ProperWaterloggedBlock.withWater(level, AllBlocks.FAKE_TRACK.getDefaultState(), pos);
            if (NeoForge.EVENT_BUS.post(new PlannedPlace(level, pos, planned, player)).isCanceled()) {
                return false;
            }
        }
        return true;
    }

    private static final class PlannedPlace extends BlockEvent.EntityPlaceEvent {
        private final BlockState planned;

        private PlannedPlace(ServerLevel level, BlockPos pos, BlockState planned, Player player) {
            super(BlockSnapshot.create(level.dimension(), level, pos), level.getBlockState(pos.below()), player);
            this.planned = planned;
        }

        @Override
        public BlockState getState() {
            return planned;
        }

        @Override
        public BlockState getPlacedBlock() {
            return planned;
        }
    }
}
