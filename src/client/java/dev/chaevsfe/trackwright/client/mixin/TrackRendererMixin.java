package dev.chaevsfe.trackwright.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.trains.track.TrackRenderer;
import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import dev.chaevsfe.trackwright.client.track.FarEnd;
import dev.chaevsfe.trackwright.client.track.TrackLight;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TrackRenderer.class)
public abstract class TrackRendererMixin {
    @WrapOperation(
        method = "extractRenderState(Lcom/zurrtum/create/content/trains/track/TrackBlockEntity;Lcom/zurrtum/create/client/content/trains/track/TrackRenderer$TrackRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At(value = "INVOKE", target = "Lcom/zurrtum/create/content/trains/track/BezierConnection;isPrimary()Z")
    )
    private boolean trackwright$drawFromFarEnd(
        BezierConnection connection,
        Operation<Boolean> original,
        @Local(argsOnly = true) TrackBlockEntity track,
        @Local(argsOnly = true) Vec3 camera
    ) {
        return original.call(connection)
            || FarEnd.firstEndHidden(track.getLevel(), connection, camera, ((TrackRenderer) (Object) this).getViewDistance());
    }

    @WrapOperation(
        method = "extractRenderState(Lcom/zurrtum/create/content/trains/track/TrackBlockEntity;Lcom/zurrtum/create/client/content/trains/track/TrackRenderer$TrackRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightCoords(Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/core/BlockPos;)I")
    )
    private int trackwright$lightInsideSolidBlocks(BlockAndLightGetter level, BlockPos pos, Operation<Integer> original) {
        return TrackLight.insideSolidBlock(level, pos, original.call(level, pos), neighbour -> original.call(level, neighbour));
    }
}
