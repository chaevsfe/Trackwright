package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.content.trains.graph.TrackGraph;
import com.zurrtum.create.content.trains.graph.TrackNodeLocation;
import dev.chaevsfe.trackwright.client.track.SplitOriginal;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Mixin(TrackGraph.class)
public abstract class TrackGraphMixin {
    @WrapOperation(
        method = "findDisconnectedGraphs",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;stream()Ljava/util/stream/Stream;")
    )
    private Stream<TrackNodeLocation> trackwright$keepTheServersOriginal(
        Set<TrackNodeLocation> vertices,
        Operation<Stream<TrackNodeLocation>> original,
        @Local(argsOnly = true) @Nullable LevelAccessor level,
        @Local(argsOnly = true) @Nullable Map<Integer, Pair<Integer, UUID>> splitSubGraphs
    ) {
        if (level == null && splitSubGraphs != null && !splitSubGraphs.isEmpty()) {
            TrackNodeLocation start = SplitOriginal.start((TrackGraph) (Object) this, vertices, splitSubGraphs.keySet());
            if (start != null) {
                return Stream.of(start);
            }
        }
        return original.call(vertices);
    }
}
