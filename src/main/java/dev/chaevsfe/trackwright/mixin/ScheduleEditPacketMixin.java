package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.trains.schedule.ScheduleEditPacket;
import dev.chaevsfe.trackwright.schedule.ActorSteps;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ScheduleEditPacket.class)
public abstract class ScheduleEditPacketMixin {
    @WrapMethod(method = "handle")
    private void trackwright$keepActorSteps(ServerPlayer sender, Operation<Void> original) {
        if (!ActorSteps.refuseEdit(sender)) {
            original.call(sender);
        }
    }
}
