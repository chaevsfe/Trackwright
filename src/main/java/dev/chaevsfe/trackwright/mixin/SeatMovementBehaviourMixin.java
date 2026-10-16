package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.contraptions.actors.seat.SeatMovementBehaviour;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.chaevsfe.trackwright.track.PassengerProtection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SeatMovementBehaviour.class)
public abstract class SeatMovementBehaviourMixin {
    @ModifyVariable(method = "visitNewPosition", at = @At("STORE"), name = "solid")
    private boolean trackwright$keepTrainPassengersSeated(boolean solid, @Local(argsOnly = true) MovementContext context) {
        return solid && !PassengerProtection.keepsSeat(context);
    }
}
