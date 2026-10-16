package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.chaevsfe.trackwright.track.PassengerProtection;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyExpressionValue(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInWall()Z"))
    private boolean trackwright$seatedPassengersStayClear(boolean inWall) {
        return inWall && !PassengerProtection.isProtected((LivingEntity) (Object) this);
    }
}
