package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.track.TrackBlockItem;
import com.simibubi.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.track.Messages;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import dev.chaevsfe.trackwright.undo.UndoRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TrackBlockItem.class)
public abstract class TrackBlockItemMixin {
    @WrapOperation(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/trains/track/TrackPlacement;tryConnect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/ItemStack;ZZ)Lcom/simibubi/create/content/trains/track/TrackPlacement$PlacementInfo;"
        )
    )
    private TrackPlacement.PlacementInfo trackwright$recordForUndo(
        Level level,
        Player player,
        BlockPos pos,
        BlockState state,
        ItemStack stack,
        boolean girder,
        boolean maximiseTurn,
        Operation<TrackPlacement.PlacementInfo> original
    ) {
        return UndoRecorder.record(level, player, stack, () -> original.call(level, player, pos, state, stack, girder, maximiseTurn));
    }

    @WrapOperation(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/CreateLang;translateDirect(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",
            ordinal = 2
        )
    )
    private MutableComponent trackwright$fallbackForVanillaClients(
        String message,
        Object[] args,
        Operation<MutableComponent> original,
        @Local(name = "info") TrackPlacement.PlacementInfo info
    ) {
        String key = "create." + message;
        MutableComponent named = Messages.named(key, info);
        if (named != null) {
            return named;
        }
        String fallback = Messages.fallback(key);
        return fallback != null ? Component.translatableWithFallback(key, fallback) : original.call(message, args);
    }

    @WrapOperation(
        method = "useOn",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V", ordinal = 2)
    )
    private void trackwright$refusalInChatForVanillaClients(
        Player player,
        Component message,
        boolean actionBar,
        Operation<Void> original,
        @Local(name = "info") TrackPlacement.PlacementInfo info
    ) {
        original.call(player, Messages.overlayFor(player, message, info), actionBar);
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        if (!extension.trackwright$valid() && (Messages.isOwn(extension.trackwright$message()) || extension.trackwright$bridged())) {
            Messages.repeatInChatWithoutTrackwright(player, message);
        }
    }
}
