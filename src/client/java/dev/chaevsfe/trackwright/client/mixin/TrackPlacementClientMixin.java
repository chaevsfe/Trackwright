package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.content.trains.track.TrackPlacementClient;
import com.zurrtum.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.client.guide.TrackGuide;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrackPlacementClient.class)
public abstract class TrackPlacementClientMixin {
    @Inject(method = "clientTick", at = @At("HEAD"))
    private static void trackwright$forgetLastPlan(Minecraft mc, CallbackInfo ci) {
        TrackGuide.forget();
    }

    @WrapOperation(
        method = "clientTick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/zurrtum/create/content/trains/track/TrackPlacement;tryConnect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/ItemStack;ZZ)Lcom/zurrtum/create/content/trains/track/TrackPlacement$PlacementInfo;",
            ordinal = 0
        )
    )
    private static TrackPlacement.PlacementInfo trackwright$planForGuide(
        Level level,
        Player player,
        BlockPos target,
        BlockState state,
        ItemStack stack,
        boolean girder,
        boolean maximiseTurn,
        Operation<TrackPlacement.PlacementInfo> original
    ) {
        TrackPlacement.PlacementInfo info = original.call(level, player, target, state, stack, girder, maximiseTurn);
        TrackGuide.capture(target, stack, info);
        return info;
    }
}
