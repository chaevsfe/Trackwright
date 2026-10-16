package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class PassengerResync {
    private static final int DELAY_TICKS = 10;
    private static final Map<AbstractContraptionEntity, Long> DUE = new WeakHashMap<>();

    private PassengerResync() {
    }

    public static void schedule(AbstractContraptionEntity carriage) {
        DUE.put(carriage, carriage.level().getGameTime() + DELAY_TICKS);
    }

    public static void tick(MinecraftServer server) {
        if (DUE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<AbstractContraptionEntity, Long>> entries = DUE.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<AbstractContraptionEntity, Long> entry = entries.next();
            AbstractContraptionEntity carriage = entry.getKey();
            if (carriage.isRemoved() || !(carriage.level() instanceof ServerLevel level)) {
                entries.remove();
                continue;
            }
            if (level.getGameTime() < entry.getValue()) {
                continue;
            }
            entries.remove();
            ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(carriage);
            for (ServerPlayer player : level.players()) {
                if (!TrackwrightNetwork.hasTrackwright(player)) {
                    player.connection.send(packet);
                }
            }
        }
    }

    public static void clear() {
        DUE.clear();
    }
}
