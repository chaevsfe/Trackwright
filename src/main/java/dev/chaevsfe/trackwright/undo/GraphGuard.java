package dev.chaevsfe.trackwright.undo;

import com.zurrtum.create.Create;
import com.zurrtum.create.content.trains.GlobalRailwayManager;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.graph.TrackEdge;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import com.zurrtum.create.content.trains.graph.TrackNode;
import com.zurrtum.create.content.trains.signal.TrackEdgePoint;
import com.zurrtum.create.content.trains.track.BezierConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

final class GraphGuard {
    private static final double POINT_REACH = 1.5;
    private static final double SAMPLE_STEP = 0.25;
    private static final double CARRIAGE_REACH = 2;
    private static final double NUDGE = 0.01;

    private GraphGuard() {
    }

    static @Nullable String check(ServerLevel level, PlacementRecord record) {
        if (record.trackCells.isEmpty()) {
            return null;
        }
        AABB box = bounds(record.trackCells);
        if (!level.getEntitiesOfClass(CarriageContraptionEntity.class, box.inflate(CARRIAGE_REACH)).isEmpty()) {
            return UndoMessages.TRAIN;
        }
        GlobalRailwayManager railways = Create.RAILWAYS;
        if (railways == null || railways.trackNetworks == null) {
            return null;
        }
        ResourceKey<Level> dimension = level.dimension();
        Set<TrackNode> nodes = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<TrackEdge> edges = Collections.newSetFromMap(new IdentityHashMap<>());
        AABB reach = box.inflate(1);
        for (TrackGraph graph : railways.trackNetworks.values()) {
            for (Map.Entry<TrackNode, Map<TrackNode, TrackEdge>> from : graph.connectionsByNode.entrySet()) {
                TrackNode node = from.getKey();
                if (!dimension.equals(node.getLocation().getDimension())) {
                    continue;
                }
                Vec3 start = node.getLocation().getLocation();
                if (atCells(record, start)) {
                    nodes.add(node);
                }
                for (Map.Entry<TrackNode, TrackEdge> to : from.getValue().entrySet()) {
                    TrackEdge edge = to.getValue();
                    if (edge.isInterDimensional()) {
                        continue;
                    }
                    if (edge.isTurn()) {
                        if (ours(record, edge.getTurn())) {
                            edges.add(edge);
                        }
                        continue;
                    }
                    Vec3 end = to.getKey().getLocation().getLocation();
                    if (reach.intersects(new AABB(start, end)) && passes(record, start, end)) {
                        edges.add(edge);
                    }
                }
            }
        }
        for (TrackEdge edge : edges) {
            for (TrackEdgePoint point : edge.getEdgeData().getPoints()) {
                if (edge.isTurn()) {
                    return UndoMessages.POINTS;
                }
                double length = edge.getLength();
                double t = length <= 0 ? 0 : point.getLocationOn(edge) / length;
                if (near(record, edge.getPosition(null, t))) {
                    return UndoMessages.POINTS;
                }
            }
        }
        if (railways.trains == null) {
            return null;
        }
        for (Train train : railways.trains.values()) {
            boolean[] on = {false};
            train.forEachTravellingPoint(point -> on[0] |= point.edge != null && edges.contains(point.edge)
                || point.node1 != null && nodes.contains(point.node1)
                || point.node2 != null && nodes.contains(point.node2));
            if (on[0]) {
                return UndoMessages.TRAIN;
            }
        }
        return null;
    }

    private static boolean ours(PlacementRecord record, @Nullable BezierConnection turn) {
        if (turn == null) {
            return false;
        }
        BlockPos first = turn.bePositions.getFirst();
        BlockPos second = turn.bePositions.getSecond();
        Set<BlockPos> added = record.newCurves.get(first);
        return added != null && added.contains(second);
    }

    private static boolean passes(PlacementRecord record, Vec3 start, Vec3 end) {
        double length = start.distanceTo(end);
        int steps = Math.max(1, (int) Math.ceil(length / SAMPLE_STEP));
        for (int i = 0; i <= steps; i++) {
            if (onCells(record, start.lerp(end, (double) i / steps))) {
                return true;
            }
        }
        return false;
    }

    private static boolean onCells(PlacementRecord record, Vec3 point) {
        return record.trackCells.contains(BlockPos.containing(point));
    }

    private static boolean atCells(PlacementRecord record, Vec3 point) {
        for (int corner = 0; corner < 8; corner++) {
            Vec3 nudged = point.add((corner & 1) == 0 ? -NUDGE : NUDGE, (corner & 2) == 0 ? -NUDGE : NUDGE, (corner & 4) == 0 ? -NUDGE : NUDGE);
            if (onCells(record, nudged)) {
                return true;
            }
        }
        return false;
    }

    private static boolean near(PlacementRecord record, Vec3 point) {
        double limit = POINT_REACH * POINT_REACH;
        for (BlockPos cell : record.trackCells) {
            if (Vec3.atBottomCenterOf(cell).distanceToSqr(point) <= limit) {
                return true;
            }
        }
        return false;
    }

    private static AABB bounds(Set<BlockPos> cells) {
        AABB box = null;
        for (BlockPos cell : cells) {
            AABB next = new AABB(cell);
            box = box == null ? next : box.minmax(next);
        }
        return box;
    }
}
