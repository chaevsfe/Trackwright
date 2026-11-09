package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.datafixers.DataFixer;
import dev.chaevsfe.trackwright.track.OldWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataFixTypes.class)
public class OldWorldDataMixin {
    @Inject(method = "update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/nbt/CompoundTag;II)Lnet/minecraft/nbt/CompoundTag;", at = @At("HEAD"))
    private void trackwright$markOldWorldData(DataFixer fixer, CompoundTag tag, int from, int to, CallbackInfoReturnable<CompoundTag> info,
                                              @Share("trackwrightUnfixed") LocalRef<Tag> unfixed) {
        unfixed.set(OldWorldData.unfixed((DataFixTypes) (Object) this, tag, from, to));
    }

    @Inject(method = "update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/nbt/CompoundTag;II)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void trackwright$fixOldWorldData(DataFixer fixer, CompoundTag tag, int from, int to, CallbackInfoReturnable<CompoundTag> info,
                                             @Share("trackwrightUnfixed") LocalRef<Tag> unfixed) {
        OldWorldData.fix((DataFixTypes) (Object) this, fixer, from, to, info.getReturnValue(), unfixed.get());
    }
}
