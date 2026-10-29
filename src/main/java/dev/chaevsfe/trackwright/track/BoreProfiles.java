package dev.chaevsfe.trackwright.track;

import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class BoreProfiles {
    private static final String WIDE_GAUGE = "com.railwayteam.railways.content.custom_tracks.wide_gauge.WideGaugeTrackBlock";
    private static final String NARROW_GAUGE = "com.railwayteam.railways.content.custom_tracks.narrow_gauge.NarrowGaugeTrackBlock";
    private static final Map<UUID, Request> REQUESTS = new ConcurrentHashMap<>();
    private static volatile @Nullable Function<Player, Request> clientSource;

    private BoreProfiles() {
    }

    public record Request(@Nullable Integer left, @Nullable Integer right, @Nullable Integer up) {
        public static final Request NONE = new Request(null, null, null);

        public BoreProfile over(BoreProfile base) {
            return new BoreProfile(left != null ? left : base.left(), right != null ? right : base.right(), up != null ? up : base.up());
        }
    }

    public static void receive(ServerPlayer player, Request request) {
        REQUESTS.put(player.getUUID(), request);
    }

    public static void forget(UUID player) {
        REQUESTS.remove(player);
    }

    public static void clearAll() {
        REQUESTS.clear();
    }

    public static void setClientSource(Function<Player, Request> source) {
        clientSource = source;
    }

    public static Request request(Level level, Player player) {
        if (level.isClientSide()) {
            Function<Player, Request> source = clientSource;
            return source == null ? Request.NONE : source.apply(player);
        }
        return REQUESTS.getOrDefault(player.getUUID(), Request.NONE);
    }

    public static BoreProfile resolve(Level level, Player player, @Nullable TrackMaterial material, TrackwrightSettings settings) {
        return request(level, player).over(defaultFor(settings, material)).clamp(settings.boreWidthMax(), settings.boreHeadroomMax());
    }

    public static BoreProfile defaultFor(TrackwrightSettings settings, @Nullable TrackMaterial material) {
        return settings.boreProfileDefault().scaleSides(gaugeWidth(material)).clamp(settings.boreWidthMax(), settings.boreHeadroomMax());
    }

    public static double gaugeWidth(@Nullable TrackMaterial material) {
        if (material == null) {
            return 1;
        }
        Block block = material.getBlock();
        for (Class<?> type = block == null ? null : block.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            String name = type.getName();
            if (WIDE_GAUGE.equals(name)) {
                return 2;
            }
            if (NARROW_GAUGE.equals(name)) {
                return 0.5;
            }
        }
        return 1;
    }
}
