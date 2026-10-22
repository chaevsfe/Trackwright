package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.graph.TrackEdge;
import dev.chaevsfe.trackwright.track.StraightConnections;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CarriageContraptionEntity.class)
public abstract class CarriageContraptionEntityMixin {
    @WrapOperation(
        method = "control",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/graph/TrackEdge;isTurn()Z")
    )
    private boolean trackwright$straightIsNotATurn(TrackEdge edge, Operation<Boolean> original) {
        return original.call(edge) && !StraightConnections.isFlatStraight(edge.getTurn());
    }
}
