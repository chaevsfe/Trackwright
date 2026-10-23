package dev.chaevsfe.trackwright.track;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackPaver;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BridgePermission {
    private BridgePermission() {
    }

    public static boolean allowed(ServerLevel level, Player player, PlacementInfo info, BezierConnection curve, BlockPos pos1, BlockPos pos2,
                                  Vec3 axis1, Vec3 axis2) {
        BlockPos origin = curve.bePositions.getFirst();
        Comparator<BlockPos> nearestFirst = Comparator.comparingDouble(pos -> pos.distSqr(origin));
        List<BlockPos> line = new ArrayList<>();
        for (Map.Entry<Pair<Integer, Integer>, Double> column : LoadedColumns.filter(level, origin, curve.rasterise()).entrySet()) {
            line.add(new BlockPos(column.getKey().getFirst(), Mth.floor(column.getValue()), column.getKey().getSecond()).offset(origin).above());
        }
        line.sort(nearestFirst);
        for (BlockPos pos : line) {
            if (refused(level, player, pos, fakeTrack(level, pos))) {
                return false;
            }
        }
        Block paving = paving(level, player, pos1);
        if (paving == null) {
            return true;
        }
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        Set<BlockPos> visited = new HashSet<>();
        TrackPaver.paveStraight(level, pos1.below(), axis1, extension.trackwright$end1Extent() + 1, paving, true, visited);
        TrackPaver.paveStraight(level, pos2.below(), axis2, extension.trackwright$end2Extent() + 1, paving, true, visited);
        TrackPaver.paveCurve(level, curve, paving, true, visited);
        List<BlockPos> paved = new ArrayList<>(visited);
        paved.sort(nearestFirst);
        for (BlockPos pos : paved) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != paving && state.canBeReplaced()
                && refused(level, player, pos, ProperWaterloggedBlock.withWater(level, paving.defaultBlockState(), pos))) {
                return false;
            }
        }
        return true;
    }

    private static BlockState fakeTrack(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        if (AllBlocks.FAKE_TRACK.has(state) || !state.canBeReplaced() || !fluid.isEmpty() && !fluid.isSourceOfType(Fluids.WATER)) {
            return state;
        }
        return ProperWaterloggedBlock.withWater(level, AllBlocks.FAKE_TRACK.getDefaultState(), pos);
    }

    private static @Nullable Block paving(ServerLevel level, Player player, BlockPos pos1) {
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof BlockItem item) || AllItemTags.INVALID_FOR_TRACK_PAVING.matches(offhand)) {
            return null;
        }
        Block block = item.getBlock();
        if (block instanceof EntityBlock || block.defaultBlockState().getCollisionShape(level, pos1).isEmpty()) {
            return null;
        }
        return block;
    }

    private static boolean refused(ServerLevel level, Player player, BlockPos pos, BlockState planned) {
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        BlockState against = level.getBlockState(pos.below());
        BlockEvent.EntityPlaceEvent event = snapshot.getState().isAir()
            ? new PlannedPlace(snapshot, against, player, planned)
            : new PlannedCell(snapshot, against, player, planned);
        return NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    private static final class PlannedPlace extends BlockEvent.EntityPlaceEvent {
        private final BlockState planned;

        private PlannedPlace(BlockSnapshot snapshot, BlockState against, Player player, BlockState planned) {
            super(snapshot, against, player);
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

    private static final class PlannedCell extends BlockEvent.EntityMultiPlaceEvent {
        private final BlockState planned;

        private PlannedCell(BlockSnapshot snapshot, BlockState against, Player player, BlockState planned) {
            super(List.of(snapshot), against, player);
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
