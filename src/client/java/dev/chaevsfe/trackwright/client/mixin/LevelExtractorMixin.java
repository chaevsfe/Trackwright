package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.chaevsfe.trackwright.client.track.FarEnd;
import dev.chaevsfe.trackwright.track.PassengerProtection;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Inject(method = "extract", at = @At("HEAD"))
    private void trackwright$beginFrame(DeltaTracker deltaTracker, Camera camera, float partialTick, CallbackInfo ci) {
        FarEnd.beginFrame();
    }

    @ModifyReturnValue(method = "getViewBlockingState", at = @At("RETURN"))
    private static @Nullable BlockState trackwright$clearViewForSeatedPassengers(@Nullable BlockState state, @Local(argsOnly = true) LocalPlayer player) {
        return state != null && PassengerProtection.isProtectedOnClient(player) ? null : state;
    }
}
