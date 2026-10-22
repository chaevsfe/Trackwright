package dev.chaevsfe.trackwright.mixin;

import com.simibubi.create.content.contraptions.Contraption;
import dev.chaevsfe.trackwright.schedule.ActorSwitch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Contraption.class)
public abstract class ContraptionMixin {
    @Inject(method = "readNBT", at = @At("TAIL"))
    private void trackwright$applyActorSteps(Level world, CompoundTag nbt, boolean spawnData, CallbackInfo ci) {
        if (!spawnData && world != null && !world.isClientSide()) {
            ActorSwitch.applyPending((Contraption) (Object) this, nbt);
        }
    }
}
