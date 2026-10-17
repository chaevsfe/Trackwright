package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.Create;
import com.zurrtum.create.content.trains.GlobalRailwayManager;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.graph.TrackEdge;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import com.zurrtum.create.content.trains.graph.TrackNode;
import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import com.zurrtum.create.content.trains.track.TrackPropagator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class BridgeConversion {
    private static final double CARRIAGE_REACH = 2;

    private BridgeConversion() {
    }

    public static @Nullable BezierConnection existing(Level level, BlockPos pos1, BlockPos pos2) {
        if (!level.isLoaded(pos1) || !(level.getBlockEntity(pos1) instanceof TrackBlockEntity track)) {
            return null;
        }
        BezierConnection connection = track.getConnections().get(pos2);
        return StraightConnections.isFlatStraight(connection) ? connection : null;
    }

    public static @Nullable String refusal(Level level, BlockPos pos1, BlockPos pos2) {
        GlobalRailwayManager railways = Create.RAILWAYS == null ? null : Create.RAILWAYS.sided(level);
        if (railways == null || railways.trackNetworks == null) {
            return null;
        }
        Set<TrackEdge> edges = Collections.newSetFromMap(new IdentityHashMap<>());
        for (TrackGraph graph : railways.trackNetworks.values()) {
            for (Map<TrackNode, TrackEdge> from : graph.connectionsByNode.values()) {
                for (TrackEdge edge : from.values()) {
                    if (edge.isTurn() && joins(edge.getTurn(), pos1, pos2)) {
                        edges.add(edge);
                    }
                }
            }
        }
        for (TrackEdge edge : edges) {
            if (edge.getEdgeData().hasPoints()) {
                return Messages.CONVERT_POINTS;
            }
        }
        if (edges.isEmpty() || level.isClientSide() || railways.trains == null) {
            return null;
        }
        if (!level.getEntitiesOfClass(CarriageContraptionEntity.class, new AABB(pos1).minmax(new AABB(pos2)).inflate(CARRIAGE_REACH)).isEmpty()) {
            return Messages.CONVERT_TRAIN;
        }
        for (Train train : railways.trains.values()) {
            boolean[] on = {false};
            train.forEachTravellingPoint(point -> on[0] |= point.edge != null && edges.contains(point.edge));
            if (on[0]) {
                return Messages.CONVERT_TRAIN;
            }
        }
        return null;
    }

    public static void remove(ServerLevel level, ServerPlayer player, BlockPos pos1, BlockPos pos2) {
        if (!(level.getBlockEntity(pos1) instanceof TrackBlockEntity first)) {
            return;
        }
        BezierConnection connection = first.getConnections().get(pos2);
        if (!StraightConnections.isFlatStraight(connection)) {
            return;
        }
        first.removeConnection(pos2);
        if (level.getBlockEntity(pos2) instanceof TrackBlockEntity second) {
            second.removeConnection(pos1);
        }
        TrackPropagator.onRailRemoved(level, pos1, first.getBlockState());
        if (!player.isCreative()) {
            connection.addItemsToPlayer(player);
        }
        connection.spawnDestroyParticles(level);
    }

    private static boolean joins(@Nullable BezierConnection turn, BlockPos pos1, BlockPos pos2) {
        if (turn == null) {
            return false;
        }
        BlockPos first = turn.bePositions.getFirst();
        BlockPos second = turn.bePositions.getSecond();
        return first.equals(pos1) && second.equals(pos2) || first.equals(pos2) && second.equals(pos1);
    }
}
