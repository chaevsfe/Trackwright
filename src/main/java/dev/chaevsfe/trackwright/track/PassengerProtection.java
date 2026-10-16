package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import dev.chaevsfe.trackwright.config.ServerConfig;
import net.minecraft.world.entity.LivingEntity;

public final class PassengerProtection {
    private PassengerProtection() {
    }

    public static boolean isProtected(LivingEntity entity) {
        return entity.getVehicle() instanceof CarriageContraptionEntity && ServerConfig.get().protectTrainPassengers();
    }
}
