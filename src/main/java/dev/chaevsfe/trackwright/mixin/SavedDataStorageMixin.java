package dev.chaevsfe.trackwright.mixin;

import dev.chaevsfe.trackwright.track.TrainBlockStateKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(SavedDataStorage.class)
public class SavedDataStorageMixin {
    @Inject(method = "readTagFromDisk(Ljava/nio/file/Path;Lnet/minecraft/util/datafix/DataFixTypes;I)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void trackwright$fixTrainBlockStateKeys(Path path, DataFixTypes type, int version, CallbackInfoReturnable<CompoundTag> info) {
        TrainBlockStateKeys.fix(path, info.getReturnValue());
    }
}
