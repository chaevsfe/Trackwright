package dev.chaevsfe.trackwright.markers;

import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import dev.chaevsfe.trackwright.network.MarkersPayload;
import dev.chaevsfe.trackwright.track.TrackItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MarkerSync {
    public static final int RADIUS_CHUNKS = 2;
    public static final int MAX_CELLS = 4096;

    private record Sent(ResourceKey<Level> dimension, long chunk, long generation) {
    }

    private static final Map<UUID, Sent> SENT = new HashMap<>();

    private MarkerSync() {
    }

    public static void tick(MinecraftServer server) {
        if (!ServerConfig.get().unbreakableMarkers().enabled()) {
            SENT.clear();
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!wantsMarkers(player) || !ServerPlayNetworking.canSend(player, MarkersPayload.TYPE)) {
                SENT.remove(player.getUUID());
                continue;
            }
            ServerLevel level = player.level();
            ChunkPos center = ChunkPos.containing(player.blockPosition());
            Sent now = new Sent(level.dimension(), center.pack(), Markers.generation(level.dimension()));
            if (now.equals(SENT.get(player.getUUID()))) {
                continue;
            }
            SENT.put(player.getUUID(), now);
            ServerPlayNetworking.send(player, snapshot(level, player.blockPosition(), center));
        }
    }

    public static boolean wantsMarkers(Player player) {
        return Markers.isMarkerTool(player.getMainHandItem())
            || BlockMiner.isTool(player.getMainHandItem())
            || TrackItems.isTrack(player.getMainHandItem())
            || TrackItems.isTrack(player.getOffhandItem());
    }

    public static MarkersPayload snapshot(ServerLevel level, BlockPos around, ChunkPos center) {
        List<BlockPos> found = new ArrayList<>();
        for (int dx = -RADIUS_CHUNKS; dx <= RADIUS_CHUNKS; dx++) {
            for (int dz = -RADIUS_CHUNKS; dz <= RADIUS_CHUNKS; dz++) {
                ChunkPos chunk = new ChunkPos(center.x() + dx, center.z() + dz);
                MarkerSet set = Markers.setAt(level, chunk.x(), chunk.z());
                if (set == null) {
                    continue;
                }
                for (int cell : set.cells()) {
                    found.add(MarkerSet.pos(chunk, cell));
                }
            }
        }
        boolean truncated = found.size() > MAX_CELLS;
        if (truncated) {
            found.sort(Comparator.comparingDouble(pos -> pos.distSqr(around)));
        }
        int count = Math.min(found.size(), MAX_CELLS);
        long[] cells = new long[count];
        for (int i = 0; i < count; i++) {
            cells[i] = found.get(i).asLong();
        }
        return MarkersPayload.of(level.dimension(), cells, truncated, center);
    }

    public static void forget(UUID player) {
        SENT.remove(player);
    }

    public static void clearAll() {
        SENT.clear();
    }
}
