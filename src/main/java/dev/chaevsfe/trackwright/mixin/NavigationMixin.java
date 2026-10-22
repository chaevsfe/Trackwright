package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.chaevsfe.trackwright.track.StraightConnections;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Navigation.class)
public abstract class NavigationMixin {
    @ModifyExpressionValue(method = "lambda$tick$1", at = @At(value = "CONSTANT", args = "doubleValue=0.0625"))
    private static double trackwright$straightIsNotATurn(double threshold, @Local(name = "turn") BezierConnection turn) {
        return StraightConnections.isFlatStraight(turn) ? -1 : threshold;
    }
}
