package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.zurrtum.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.track.Messages;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import dev.chaevsfe.trackwright.track.PlacementRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrackPlacement.class)
public abstract class TrackPlacementMixin {
    @ModifyExpressionValue(
        method = "tryConnect",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;equal(DD)Z", ordinal = 4)
    )
    private static boolean trackwright$bridgeBlockedStraight(
        boolean flat,
        @Local(name = "level") Level level,
        @Local(name = "info") TrackPlacement.PlacementInfo info,
        @Local(name = "pos1") BlockPos pos1,
        @Local(name = "axis1") Vec3 axis1,
        @Local(name = "axis2") Vec3 axis2,
        @Local(name = "skipCurve") LocalBooleanRef skipCurve
    ) {
        if (!flat || !skipCurve.get()) {
            return flat;
        }
        switch (PlacementRules.decideStraight(level, pos1, axis1, axis2, info.end1Extent)) {
            case BRIDGE -> {
                info.end1Extent = 0;
                info.end2Extent = 0;
                skipCurve.set(false);
                ((PlacementInfoExtension) info).trackwright$setBridged(true);
            }
            case OCCUPIED -> ((PlacementInfoExtension) info).trackwright$setRefusal(Messages.LINE_OCCUPIED);
            case KEEP -> {
            }
        }
        return flat;
    }

    @Inject(
        method = "tryConnect",
        at = @At(
            value = "FIELD",
            target = "Lcom/zurrtum/create/content/trains/track/TrackPlacement$PlacementInfo;valid:Z",
            opcode = Opcodes.PUTFIELD,
            ordinal = 0,
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private static void trackwright$refuseOverlaps(
        CallbackInfoReturnable<TrackPlacement.PlacementInfo> cir,
        @Local(name = "level") Level level,
        @Local(name = "info") TrackPlacement.PlacementInfo info,
        @Local(name = "pos1") BlockPos pos1,
        @Local(name = "pos2") BlockPos pos2,
        @Local(name = "axis1") Vec3 axis1,
        @Local(name = "axis2") Vec3 axis2
    ) {
        String refusal = PlacementRules.refusal(level, info, pos1, pos2, axis1, axis2);
        if (refusal != null) {
            info.valid = false;
            cir.setReturnValue(info.withMessage(refusal).tooJumbly());
        }
    }
}
