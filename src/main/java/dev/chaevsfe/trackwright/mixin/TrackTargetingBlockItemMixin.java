package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;
import dev.chaevsfe.trackwright.track.ConnectionLength;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TrackTargetingBlockItem.class)
public abstract class TrackTargetingBlockItemMixin {
    @ModifyExpressionValue(
        method = "useOn",
        at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;")
    )
    private Object trackwright$reachAlongLongCurve(Object value, @Local(argsOnly = true) UseOnContext context) {
        return ConnectionLength.forTargeting(value, context);
    }
}
