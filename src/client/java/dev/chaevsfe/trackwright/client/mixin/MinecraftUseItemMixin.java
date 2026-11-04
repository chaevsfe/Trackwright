package dev.chaevsfe.trackwright.client.mixin;

import dev.chaevsfe.trackwright.client.input.UseItemHooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftUseItemMixin {
    @Inject(
        method = "startUseItem()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/InteractionHand;values()[Lnet/minecraft/world/InteractionHand;"),
        cancellable = true
    )
    private void trackwright$beforeUseItem(CallbackInfo ci) {
        if (UseItemHooks.beforeUseItem((Minecraft) (Object) this)) {
            ci.cancel();
        }
    }
}
