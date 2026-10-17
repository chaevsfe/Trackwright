package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.zurrtum.create.content.trains.station.StationBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(StationBlockEntity.class)
public abstract class StationBlockEntityMixin {
    @WrapMethod(method = "tryEnterAssemblyMode")
    private boolean trackwright$noAssemblyModeOnCurve(Operation<Boolean> original) {
        return !trackwright$onCurve() && original.call();
    }

    @WrapMethod(method = "tryDisassembleTrain")
    private boolean trackwright$noDisassemblyOnCurve(@Nullable ServerPlayer sender, Operation<Boolean> original) {
        return !trackwright$onCurve() && original.call(sender);
    }

    @WrapMethod(method = "assemble")
    private void trackwright$noAssemblyOnCurve(UUID playerUUID, Operation<Void> original) {
        if (!trackwright$onCurve()) {
            original.call(playerUUID);
        }
    }

    @Unique
    private boolean trackwright$onCurve() {
        StationBlockEntity station = (StationBlockEntity) (Object) this;
        return station.edgePoint != null && station.edgePoint.isOnCurve();
    }
}
