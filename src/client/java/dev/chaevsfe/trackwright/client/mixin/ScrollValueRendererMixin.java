package dev.chaevsfe.trackwright.client.mixin;

import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueRenderer;
import dev.chaevsfe.trackwright.client.actors.RollerTerrainScroll;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScrollValueRenderer.class)
public abstract class ScrollValueRendererMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private static void trackwright$rollerTerrainBox(Minecraft minecraft, CallbackInfo ci) {
        RollerTerrainScroll.tick(minecraft);
    }
}
