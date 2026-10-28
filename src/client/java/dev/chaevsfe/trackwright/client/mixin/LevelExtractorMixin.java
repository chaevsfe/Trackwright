package dev.chaevsfe.trackwright.client.mixin;

import dev.chaevsfe.trackwright.client.track.FarEnd;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelExtractorMixin {
    @Inject(method = "extractLevel", at = @At("HEAD"))
    private void trackwright$beginFrame(DeltaTracker deltaTracker, Camera camera, float partialTick, CallbackInfo ci) {
        FarEnd.beginFrame();
    }
}
