package dev.chaevsfe.trackwright.client.mixin;

import com.simibubi.create.content.trains.track.TrackBlockItem;
import dev.chaevsfe.trackwright.client.guide.WidestTurnLock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrackBlockItem.class)
public abstract class TrackBlockItemClientMixin {
    @Inject(method = "useOn", at = @At("HEAD"))
    private void trackwright$sendLockedWidestTurn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        WidestTurnLock.beforeUse(context);
    }
}
