package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.chaevsfe.trackwright.track.PassengerProtection;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @ModifyReturnValue(method = "getViewBlockingState", at = @At("RETURN"))
    private static @Nullable BlockState trackwright$clearViewForSeatedPassengers(@Nullable BlockState state, @Local(argsOnly = true) Player player) {
        return state != null && PassengerProtection.isProtectedOnClient(player) ? null : state;
    }
}
