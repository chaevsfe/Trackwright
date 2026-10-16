package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.config.Sides;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

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

    public static boolean keepsSeat(MovementContext context) {
        if (!(context.contraption.entity instanceof CarriageContraptionEntity carriage)) {
            return false;
        }
        TrackwrightSettings settings = Sides.forLevel(context.world);
        if (settings == null || !settings.protectTrainPassengers()) {
            return false;
        }
        Entity rider = rider(carriage, context);
        if (rider == null) {
            return false;
        }
        if (context.world.isClientSide()) {
            return true;
        }
        if (rider instanceof ServerPlayer player && !TrackwrightNetwork.hasTrackwright(player)) {
            return false;
        }
        PassengerResync.schedule(carriage);
        return true;
    }

    private static @Nullable Entity rider(CarriageContraptionEntity carriage, MovementContext context) {
        int seat = context.data.getIntOr("SeatIndex", -1);
        if (seat < 0) {
            return null;
        }
        for (Map.Entry<UUID, Integer> entry : context.contraption.getSeatMapping().entrySet()) {
            if (entry.getValue() != seat) {
                continue;
            }
            for (Entity passenger : carriage.getPassengers()) {
                if (passenger.getUUID().equals(entry.getKey())) {
                    return passenger;
                }
            }
        }
        return null;
    }
}
