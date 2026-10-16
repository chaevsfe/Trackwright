package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import dev.chaevsfe.trackwright.track.LoadedColumns;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(TrackBlockEntity.class)
public abstract class TrackBlockEntityMixin {
    @WrapOperation(
        method = "manageFakeTracksAlong",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/trains/track/BezierConnection;rasterise()Ljava/util/Map;")
    )
    private Map<Pair<Integer, Integer>, Double> trackwright$loadedColumnsOnly(
        BezierConnection connection,
        Operation<Map<Pair<Integer, Integer>, Double>> original
    ) {
        return LoadedColumns.filter(((BlockEntity) (Object) this).getLevel(), connection.bePositions.getFirst(), original.call(connection));
    }
}
