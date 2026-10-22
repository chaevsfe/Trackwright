package dev.chaevsfe.trackwright.mixin;

import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(TrackGraph.class)
public interface TrackGraphAccessor {
    @Accessor("connectionsByNode")
    Map<TrackNode, Map<TrackNode, TrackEdge>> trackwright$connectionsByNode();
}
