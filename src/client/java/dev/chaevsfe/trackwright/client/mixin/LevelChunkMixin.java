package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.chaevsfe.trackwright.client.track.FarEnd;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @WrapOperation(method = "setBlockEntity", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object trackwright$blockEntityLoaded(Map<Object, Object> blockEntities, Object pos, Object loaded, Operation<Object> original) {
        Object replaced = original.call(blockEntities, pos, loaded);
        if (trackwright$clientSide()) {
            if (replaced instanceof BlockEntity old && old != loaded) {
                FarEnd.endChanged(old);
            }
            if (loaded instanceof BlockEntity blockEntity) {
                FarEnd.endChanged(blockEntity);
            }
        }
        return replaced;
    }

    @WrapOperation(
        method = {
            "removeBlockEntity",
            "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;"
        },
        at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0)
    )
    private Object trackwright$blockEntityUnloaded(Map<Object, Object> blockEntities, Object pos, Operation<Object> original) {
        Object removed = original.call(blockEntities, pos);
        if (removed instanceof BlockEntity blockEntity && trackwright$clientSide()) {
            FarEnd.endChanged(blockEntity);
        }
        return removed;
    }

    @Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
    private void trackwright$blockEntitiesUnloaded(CallbackInfo ci) {
        if (trackwright$clientSide()) {
            ((LevelChunk) (Object) this).getBlockEntities().values().forEach(FarEnd::endChanged);
        }
    }

    @Unique
    private boolean trackwright$clientSide() {
        return ((LevelChunk) (Object) this).getLevel().isClientSide();
    }
}
