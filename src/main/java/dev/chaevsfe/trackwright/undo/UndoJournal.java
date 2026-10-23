package dev.chaevsfe.trackwright.undo;

import com.simibubi.create.content.trains.track.TrackBlock;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import dev.chaevsfe.trackwright.track.BridgePermission;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class UndoJournal {
    public static final int DEPTH = 3;
    public static final int WINDOW_TICKS = 2 * 60 * 20;
    private static final int SETTLE_TICKS = 4;
    private static final int REQUEST_GAP_TICKS = 10;

    private static final Map<UUID, Deque<PlacementRecord>> JOURNALS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_REQUEST = new HashMap<>();
    private static final Map<UUID, PlacementRecord> AWAITING_PLACE_EVENT = new HashMap<>();

    private UndoJournal() {
    }

    static void push(ServerPlayer player, PlacementRecord record) {
        Deque<PlacementRecord> journal = JOURNALS.computeIfAbsent(player.getUUID(), uuid -> new ArrayDeque<>());
        journal.addFirst(record);
        while (journal.size() > DEPTH) {
            journal.removeLast();
        }
        sendState(player);
    }

    static void awaitPlaceEvent(ServerPlayer player, PlacementRecord record) {
        AWAITING_PLACE_EVENT.put(player.getUUID(), record);
    }

    public static void placeEventPosted(BlockEvent.EntityPlaceEvent event) {
        if (AWAITING_PLACE_EVENT.isEmpty() || BridgePermission.planned(event) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlacementRecord record = AWAITING_PLACE_EVENT.remove(player.getUUID());
        if (record == null || !event.isCanceled()) {
            return;
        }
        List<BlockSnapshot> snapshots = event instanceof BlockEvent.EntityMultiPlaceEvent multi
            ? multi.getReplacedBlockSnapshots()
            : List.of(event.getBlockSnapshot());
        for (BlockSnapshot snapshot : snapshots) {
            PlacementRecord.Cell cell = record.cells.get(snapshot.getPos());
            if (cell != null && cell.placed()) {
                remove(player, record);
                return;
            }
        }
    }

    static @Nullable PlacementRecord newest(ServerPlayer player) {
        Deque<PlacementRecord> journal = JOURNALS.get(player.getUUID());
        if (journal == null) {
            return null;
        }
        prune(journal, player.level().getServer().getTickCount());
        return journal.peekFirst();
    }

    static void remove(ServerPlayer player, PlacementRecord record) {
        Deque<PlacementRecord> journal = JOURNALS.get(player.getUUID());
        if (journal != null) {
            journal.remove(record);
            if (journal.isEmpty()) {
                JOURNALS.remove(player.getUUID());
            }
        }
        sendState(player);
    }

    public static void clear(ServerPlayer player) {
        if (JOURNALS.remove(player.getUUID()) != null) {
            sendState(player);
        }
    }

    public static void respawned(ServerPlayer player) {
        Deque<PlacementRecord> journal = JOURNALS.get(player.getUUID());
        PlacementRecord newest = journal == null ? null : journal.peekFirst();
        if (newest != null && !newest.dimension.equals(player.level().dimension())) {
            clear(player);
        }
    }

    public static void forget(UUID player) {
        JOURNALS.remove(player);
        LAST_REQUEST.remove(player);
        AWAITING_PLACE_EVENT.remove(player);
    }

    public static void clearAll() {
        JOURNALS.clear();
        LAST_REQUEST.clear();
        AWAITING_PLACE_EVENT.clear();
    }

    public static boolean acceptRequest(ServerPlayer player) {
        int now = player.level().getServer().getTickCount();
        Integer last = LAST_REQUEST.get(player.getUUID());
        if (last != null && now - last < REQUEST_GAP_TICKS && now >= last) {
            return false;
        }
        LAST_REQUEST.put(player.getUUID(), now);
        return true;
    }

    public static int ticksLeft(PlacementRecord record, int now) {
        return Math.max(0, record.placedTick + WINDOW_TICKS - now);
    }

    public static void tick(MinecraftServer server) {
        AWAITING_PLACE_EVENT.clear();
        if (JOURNALS.isEmpty()) {
            return;
        }
        int now = server.getTickCount();
        Iterator<Deque<PlacementRecord>> journals = JOURNALS.values().iterator();
        while (journals.hasNext()) {
            Deque<PlacementRecord> journal = journals.next();
            prune(journal, now);
            if (journal.isEmpty()) {
                journals.remove();
                continue;
            }
            for (PlacementRecord record : journal) {
                if (record.settled) {
                    continue;
                }
                ServerLevel level = server.getLevel(record.dimension);
                if (level == null || level.getGameTime() - record.placedGameTime >= SETTLE_TICKS) {
                    settle(level, record);
                }
            }
        }
    }

    private static void prune(Deque<PlacementRecord> journal, int now) {
        journal.removeIf(record -> now - record.placedTick > WINDOW_TICKS);
    }

    private static void settle(@Nullable ServerLevel level, PlacementRecord record) {
        record.settled = true;
        if (level == null) {
            return;
        }
        for (Map.Entry<BlockPos, PlacementRecord.Cell> entry : record.cells.entrySet()) {
            BlockPos pos = entry.getKey();
            PlacementRecord.Cell cell = entry.getValue();
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState now = level.getBlockState(pos);
            if (cell.placed() && BlockSnapshots.fake(cell.after)) {
                continue;
            }
            boolean sameBlock = now.getBlock() == cell.after.getBlock();
            boolean sameShape = !now.hasProperty(TrackBlock.SHAPE) || !cell.after.hasProperty(TrackBlock.SHAPE)
                || now.getValue(TrackBlock.SHAPE) == cell.after.getValue(TrackBlock.SHAPE);
            boolean sameCurves = !cell.placed() || BlockSnapshots.connections(level, pos).equals(cell.afterConnections);
            if (sameBlock && sameShape && sameCurves) {
                cell.after = now;
                cell.afterKey = BlockSnapshots.key(level, pos);
            } else if (cell.placed()) {
                record.changed = true;
            }
        }
    }

    static void sendState(ServerPlayer player) {
        sendState(player, false);
    }

    static void sendResult(ServerPlayer player) {
        sendState(player, true);
    }

    private static void sendState(ServerPlayer player, boolean result) {
        Deque<PlacementRecord> journal = JOURNALS.get(player.getUUID());
        PlacementRecord newest = journal == null ? null : journal.peekFirst();
        int now = player.level().getServer().getTickCount();
        TrackwrightNetwork.sendUndoState(player, journal == null ? 0 : journal.size(), newest == null ? 0 : ticksLeft(newest, now), result);
    }
}
