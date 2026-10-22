package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
        if (trackwright$onCurve()) {
            trackwright$tell(sender);
            return false;
        }
        return original.call(sender);
    }

    @WrapMethod(method = "assemble")
    private void trackwright$noAssemblyOnCurve(UUID playerUUID, Operation<Void> original) {
        if (!trackwright$onCurve()) {
            original.call(playerUUID);
            return;
        }
        StationBlockEntity station = (StationBlockEntity) (Object) this;
        if (playerUUID != null && station.getLevel() instanceof ServerLevel level) {
            trackwright$tell(level.getServer().getPlayerList().getPlayer(playerUUID));
        }
    }

    @Unique
    private boolean trackwright$onCurve() {
        StationBlockEntity station = (StationBlockEntity) (Object) this;
        return station.edgePoint != null && station.edgePoint.isOnCurve();
    }

    @Unique
    private static void trackwright$tell(@Nullable ServerPlayer player) {
        if (player != null) {
            player.sendOverlayMessage(Component.translatableWithFallback("trackwright.station.on_curve",
                "Trains cannot be built or taken apart at a station on a curve").withStyle(ChatFormatting.RED));
        }
    }
}
