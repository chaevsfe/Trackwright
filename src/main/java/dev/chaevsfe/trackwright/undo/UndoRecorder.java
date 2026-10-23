package dev.chaevsfe.trackwright.undo;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackPlacement.ConnectingFrom;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class UndoRecorder {
    private static @Nullable Session session;

    private UndoRecorder() {
    }

    private static final class Session {
        final ServerLevel level;
        final ServerPlayer player;
        final Thread thread = Thread.currentThread();
        final @Nullable ConnectingFrom from;
        final Item trackItem;
        final Map<BlockPos, PlacementRecord.Cell> cells = new LinkedHashMap<>();
        final List<ItemStack> charged = new ArrayList<>();
        @Nullable String refusal;

        Session(ServerLevel level, ServerPlayer player, @Nullable ConnectingFrom from, Item trackItem) {
            this.level = level;
            this.player = player;
            this.from = from;
            this.trackItem = trackItem;
        }

        void touch(BlockPos pos, PlacementRecord.Kind kind) {
            BlockPos key = pos.immutable();
            PlacementRecord.Cell cell = cells.get(key);
            if (cell != null && cell.kind != PlacementRecord.Kind.NEIGHBOUR) {
                if (kind == PlacementRecord.Kind.WRITTEN) {
                    cell.kind = kind;
                }
                return;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = key.relative(direction);
                if (!cells.containsKey(next) && level.isLoaded(next)) {
                    cells.put(next, snapshot(next, PlacementRecord.Kind.NEIGHBOUR));
                }
            }
            if (cell == null) {
                cells.put(key, snapshot(key, kind));
            } else {
                cell.kind = kind;
            }
        }

        PlacementRecord.Cell snapshot(BlockPos pos, PlacementRecord.Kind kind) {
            return new PlacementRecord.Cell(kind, level.getBlockState(pos), BlockSnapshots.full(level, pos), BlockSnapshots.key(level, pos),
                BlockSnapshots.connections(level, pos));
        }

        void rebase() {
            for (Map.Entry<BlockPos, PlacementRecord.Cell> entry : cells.entrySet()) {
                BlockPos pos = entry.getKey();
                entry.getValue().rebase(level.getBlockState(pos), BlockSnapshots.full(level, pos), BlockSnapshots.key(level, pos),
                    BlockSnapshots.connections(level, pos));
            }
        }

        @Nullable PlacementRecord finish() {
            boolean changed = !charged.isEmpty();
            Set<BlockPos> tracks = new HashSet<>();
            Map<BlockPos, Set<BlockPos>> curves = new HashMap<>();
            for (Map.Entry<BlockPos, PlacementRecord.Cell> entry : cells.entrySet()) {
                BlockPos pos = entry.getKey();
                PlacementRecord.Cell cell = entry.getValue();
                cell.after = level.getBlockState(pos);
                cell.afterKey = BlockSnapshots.key(level, pos);
                cell.afterConnections = BlockSnapshots.connections(level, pos);
                if (!cell.placed()) {
                    continue;
                }
                changed |= cell.after != cell.before || !BlockSnapshots.sameKey(cell.beforeKey, cell.afterKey);
                if (cell.after.getBlock() instanceof ITrackBlock) {
                    tracks.add(pos);
                }
                Set<BlockPos> added = new HashSet<>(cell.afterConnections);
                added.removeAll(cell.beforeConnections);
                if (!added.isEmpty()) {
                    curves.put(pos, added);
                }
            }
            if (!changed) {
                return null;
            }
            Set<BlockPos> watched = new HashSet<>();
            for (BlockPos track : tracks) {
                for (BlockPos next : BlockPos.betweenClosed(track.offset(-1, -1, -1), track.offset(1, 1, 1))) {
                    PlacementRecord.Cell cell = cells.get(next);
                    if (cell != null && cell.placed() || !level.isLoaded(next)) {
                        continue;
                    }
                    if (!(level.getBlockState(next).getBlock() instanceof ITrackBlock)) {
                        watched.add(next.immutable());
                    }
                }
            }
            return new PlacementRecord(level.dimension(), level.getServer().getTickCount(), level.getGameTime(), cells, tracks, watched, curves,
                List.copyOf(charged), from, trackItem, refusal);
        }
    }

    public static PlacementInfo record(Level level, Player player, ItemStack stack, Supplier<PlacementInfo> placement) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer) || player instanceof DeployerFakePlayer
            || session != null) {
            return placement.get();
        }
        Session started = new Session(serverLevel, serverPlayer, stack.get(AllDataComponents.TRACK_CONNECTING_FROM), stack.getItem());
        session = started;
        PlacementInfo info;
        try {
            info = placement.get();
        } finally {
            session = null;
        }
        if (((PlacementInfoExtension) info).trackwright$valid() && started.cells.values().stream().anyMatch(PlacementRecord.Cell::placed)) {
            PlacementRecord record = started.finish();
            if (record != null) {
                UndoJournal.push(serverPlayer, record);
                if (serverLevel.captureBlockSnapshots) {
                    UndoJournal.awaitPlaceEvent(serverPlayer, record);
                }
            }
        }
        return info;
    }

    private static @Nullable Session active(Level level) {
        Session current = session;
        return current != null && current.level == level && current.thread == Thread.currentThread() ? current : null;
    }

    public static void beforeWrite(Level level, BlockPos pos) {
        Session current = active(level);
        if (current != null) {
            current.touch(pos, PlacementRecord.Kind.WRITTEN);
        }
    }

    public static void beforeMining(Level level, ServerPlayer player, Collection<BlockPos> cells) {
        Session current = active(level);
        if (current == null || current.player != player) {
            return;
        }
        for (BlockPos pos : cells) {
            current.touch(pos, PlacementRecord.Kind.MINED);
        }
    }

    public static void afterMining(Level level, ServerPlayer player) {
        Session current = active(level);
        if (current == null || current.player != player) {
            return;
        }
        current.rebase();
    }

    public static void charged(Player player, ItemStack slot, int remaining) {
        Session current = session;
        if (current == null || current.player != player || current.thread != Thread.currentThread()) {
            return;
        }
        int taken = slot.getCount() - remaining;
        if (taken > 0) {
            current.charged.add(slot.copyWithCount(taken));
        }
    }

    public static void notUndoable(Level level, String reason) {
        Session current = active(level);
        if (current != null && current.refusal == null) {
            current.refusal = reason;
        }
    }
}
