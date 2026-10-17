package dev.chaevsfe.trackwright.client.track;

import com.zurrtum.create.content.trains.graph.TrackGraph;
import com.zurrtum.create.content.trains.graph.TrackNode;
import com.zurrtum.create.content.trains.graph.TrackNodeLocation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class SplitOriginal {
    private SplitOriginal() {
    }

    public static @Nullable TrackNodeLocation start(TrackGraph graph, Set<TrackNodeLocation> vertices, Set<Integer> splitNodes) {
        if (vertices.size() != graph.getNodes().size()) {
            return null;
        }
        Set<TrackNodeLocation> seen = new HashSet<>();
        boolean first = true;
        for (TrackNodeLocation candidate : vertices) {
            if (!seen.add(candidate)) {
                continue;
            }
            boolean split = false;
            ArrayDeque<TrackNodeLocation> frontier = new ArrayDeque<>();
            frontier.add(candidate);
            while (!frontier.isEmpty()) {
                TrackNode node = graph.locateNode(frontier.poll());
                if (node == null) {
                    continue;
                }
                split |= splitNodes.contains(node.getNetId());
                for (TrackNode next : graph.getConnectionsFrom(node).keySet()) {
                    TrackNodeLocation location = next.getLocation();
                    if (vertices.contains(location) && seen.add(location)) {
                        frontier.add(location);
                    }
                }
            }
            if (!split) {
                return first ? null : candidate;
            }
            first = false;
        }
        return null;
    }
}
