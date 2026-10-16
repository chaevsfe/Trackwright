package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.trains.track.TrackBlockItem;
import dev.chaevsfe.trackwright.track.Messages;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TrackBlockItem.class)
public abstract class TrackBlockItemMixin {
    @WrapOperation(
        method = "useOn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;")
    )
    private MutableComponent trackwright$fallbackForVanillaClients(String key, Operation<MutableComponent> original) {
        String fallback = Messages.fallback(key);
        return fallback != null ? Component.translatableWithFallback(key, fallback) : original.call(key);
    }
}
