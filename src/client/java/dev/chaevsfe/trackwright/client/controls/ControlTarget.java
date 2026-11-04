package dev.chaevsfe.trackwright.client.controls;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public record ControlTarget(UUID trainId, int carriageIndex, BlockPos localPos, String actorId, ItemStack filter) {
    public static final String ALL_ACTORS = "trackwright.schedule.contraption_controls.all";

    public boolean same(ControlTarget other) {
        return trainId.equals(other.trainId) && carriageIndex == other.carriageIndex && localPos.equals(other.localPos);
    }

    public Component actorName() {
        return filter.isEmpty() ? Component.translatable(ALL_ACTORS) : filter.getHoverName();
    }

    public int carriageNumber() {
        return carriageIndex + 1;
    }
}
