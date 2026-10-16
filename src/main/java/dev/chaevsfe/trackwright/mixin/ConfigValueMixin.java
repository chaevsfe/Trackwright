package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.chaevsfe.trackwright.track.ConnectionLength;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.zurrtum.create.catnip.config.ConfigBase$CValue")
public abstract class ConfigValueMixin {
    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private Object trackwright$reachAlongLongCurve(Object value) {
        return ConnectionLength.duringCurveReach(this, value);
    }
}
