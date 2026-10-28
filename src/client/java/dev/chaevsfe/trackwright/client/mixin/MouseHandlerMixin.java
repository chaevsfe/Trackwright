package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.chaevsfe.trackwright.client.guide.BoreShaping;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(
        method = "onScroll",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getInventory()Lnet/minecraft/world/entity/player/Inventory;"),
        cancellable = true
    )
    private void trackwright$shapeBore(long handle, double xoffset, double yoffset, CallbackInfo ci, @Local(name = "wheel") int wheel) {
        if (BoreShaping.onScroll(wheel)) {
            ci.cancel();
        }
    }
}
