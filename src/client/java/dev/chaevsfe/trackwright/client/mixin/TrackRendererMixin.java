package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackRenderer;
import dev.chaevsfe.trackwright.client.track.FarEnd;
import dev.chaevsfe.trackwright.client.track.TrackLight;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrackRenderer.class)
public abstract class TrackRendererMixin {
    @Inject(
        method = "renderSafe(Lcom/simibubi/create/content/trains/track/TrackBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
        at = @At("HEAD")
    )
    private void trackwright$drawn(TrackBlockEntity track, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        FarEnd.drawn(track);
    }

    @WrapOperation(
        method = "renderBezierTurn",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/track/BezierConnection;isPrimary()Z")
    )
    private static boolean trackwright$drawFromFarEnd(BezierConnection connection, Operation<Boolean> original, @Local(argsOnly = true) Level level) {
        return original.call(connection) || FarEnd.firstEndHidden(level, connection);
    }

    @WrapOperation(
        method = {"renderBezierTurn", "renderGirder"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"
        )
    )
    private static int trackwright$lightInsideSolidBlocks(BlockAndTintGetter level, BlockPos pos, Operation<Integer> original) {
        return TrackLight.insideSolidBlock(level, pos, original.call(level, pos), neighbour -> original.call(level, neighbour));
    }
}
