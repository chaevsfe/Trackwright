package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class PassengerProtection {
    private PassengerProtection() {
    }

    public static boolean isProtected(LivingEntity entity) {
        return entity.getVehicle() instanceof CarriageContraptionEntity && ServerConfig.get().protectTrainPassengers();
    }

    public static boolean isProtectedOnClient(Entity entity) {
        if (!(entity.getVehicle() instanceof CarriageContraptionEntity)) {
            return false;
        }
        TrackwrightSettings settings = RemoteSettings.get();
        return settings != null && settings.protectTrainPassengers();
    }
}
