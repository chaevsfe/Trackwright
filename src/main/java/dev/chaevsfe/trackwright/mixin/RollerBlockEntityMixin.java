package dev.chaevsfe.trackwright.mixin;

import com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.chaevsfe.trackwright.actors.RollerTerrainBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(RollerBlockEntity.class)
public abstract class RollerBlockEntityMixin {
    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void trackwright$addTerrainBox(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        behaviours.add(new RollerTerrainBehaviour((SmartBlockEntity) (Object) this));
    }
}
