package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.chaevsfe.trackwright.client.track.FarEnd;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {
    @Unique
    private final List<LevelChunk> trackwright$heldBeforeResize = new ArrayList<>();

    @WrapOperation(method = "updateViewRadius", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;getPos()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos trackwright$rememberHeldChunk(LevelChunk chunk, Operation<ChunkPos> original) {
        trackwright$heldBeforeResize.add(chunk);
        return original.call(chunk);
    }

    @Inject(method = "updateViewRadius", at = @At("TAIL"))
    private void trackwright$forgetDroppedChunks(int viewDistance, CallbackInfo ci) {
        ClientChunkCache cache = (ClientChunkCache) (Object) this;
        for (LevelChunk chunk : trackwright$heldBeforeResize) {
            ChunkPos pos = chunk.getPos();
            if (cache.getChunk(pos.x, pos.z, ChunkStatus.FULL, false) != chunk) {
                chunk.getBlockEntities().values().forEach(FarEnd::endUnloaded);
            }
        }
        trackwright$heldBeforeResize.clear();
    }
}
