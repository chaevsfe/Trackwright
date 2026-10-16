package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.trains.track.TrackBlockItem;
import com.zurrtum.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.track.Messages;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
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

    @WrapOperation(
        method = "useOn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;sendOverlayMessage(Lnet/minecraft/network/chat/Component;)V", ordinal = 2)
    )
    private void trackwright$refusalInChatForVanillaClients(
        Player player,
        Component message,
        Operation<Void> original,
        @Local(name = "info") TrackPlacement.PlacementInfo info
    ) {
        original.call(player, message);
        if (!info.valid && (Messages.isOwn(info.message) || ((PlacementInfoExtension) info).trackwright$bridged())) {
            Messages.repeatInChatWithoutTrackwright(player, message);
        }
    }
}
