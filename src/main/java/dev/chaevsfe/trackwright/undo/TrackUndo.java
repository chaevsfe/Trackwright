package dev.chaevsfe.trackwright.undo;

import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import com.zurrtum.create.content.trains.track.TrackPropagator;
import dev.chaevsfe.trackwright.Trackwright;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TrackUndo {
    private static final double REACH = 32;

    private TrackUndo() {
    }

    public record Result(boolean done, Component message) {
    }

    public static Result undoAndTell(ServerPlayer player) {
        Result result = undo(player);
        UndoJournal.sendResult(player);
        player.sendOverlayMessage(result.message());
        if (result.done()) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.75f, 1);
        } else {
            AllSoundEvents.DENY.playFrom(player, 1, 1);
        }
        return result;
    }

    public static Result undo(ServerPlayer player) {
        PlacementRecord record = UndoJournal.newest(player);
        if (record == null) {
            return refused(UndoMessages.NOTHING);
        }
        String refusal = refusal(player, record);
        if (refusal != null) {
            return refused(refusal);
        }
        ServerLevel level = player.level();
        try {
            apply(level, player, record);
        } finally {
            UndoJournal.remove(player, record);
        }
        List<ItemStack> returned = new ArrayList<>(record.charged);
        return new Result(true, UndoMessages.done(returned));
    }

    private static Result refused(String key) {
        return new Result(false, UndoMessages.refusal(key));
    }

    private static @Nullable String refusal(ServerPlayer player, PlacementRecord record) {
        if (record.refusal != null) {
            return record.refusal;
        }
        if (record.changed) {
            return UndoMessages.CHANGED;
        }
        ServerLevel level = player.level();
        if (!level.dimension().equals(record.dimension)) {
            return UndoMessages.OTHER_DIMENSION;
        }
        if (!player.isAlive() || !player.mayBuild()) {
            return UndoMessages.NOT_ALLOWED;
        }
        for (BlockPos pos : record.cells.keySet()) {
            if (!level.isLoaded(pos)) {
                return UndoMessages.NOT_LOADED;
            }
        }
        for (BlockPos pos : record.watched) {
            if (!level.isLoaded(pos)) {
                return UndoMessages.NOT_LOADED;
            }
        }
        if (!withinReach(player, record)) {
            return UndoMessages.TOO_FAR;
        }
        for (Map.Entry<BlockPos, PlacementRecord.Cell> entry : record.cells.entrySet()) {
            PlacementRecord.Cell cell = entry.getValue();
            if (cell.placed() && !unchanged(level, entry.getKey(), cell)) {
                return UndoMessages.CHANGED;
            }
        }
        for (BlockPos pos : record.watched) {
            if (level.getBlockState(pos).getBlock() instanceof ITrackBlock) {
                return UndoMessages.JOINED;
            }
        }
        String graph = GraphGuard.check(level, record);
        if (graph != null) {
            return graph;
        }
        List<BlockPos> restored = restored(level, record);
        if (inTheWay(level, record, restored)) {
            return UndoMessages.IN_THE_WAY;
        }
        if (!permitted(level, player, restored)) {
            return UndoMessages.PROTECTED;
        }
        return null;
    }

    private static boolean withinReach(ServerPlayer player, PlacementRecord record) {
        Vec3 eye = player.getEyePosition();
        double limit = REACH * REACH;
        for (Map.Entry<BlockPos, PlacementRecord.Cell> entry : record.cells.entrySet()) {
            if (entry.getValue().placed() && Vec3.atCenterOf(entry.getKey()).distanceToSqr(eye) <= limit) {
                return true;
            }
        }
        return false;
    }

    private static boolean unchanged(ServerLevel level, BlockPos pos, PlacementRecord.Cell cell) {
        BlockState now = level.getBlockState(pos);
        if (BlockSnapshots.fake(cell.after)) {
            return BlockSnapshots.fake(now) || now.isAir();
        }
        return now == cell.after && BlockSnapshots.sameKey(cell.afterKey, BlockSnapshots.key(level, pos));
    }

    private static boolean restorable(ServerLevel level, BlockPos pos, PlacementRecord.Cell cell) {
        if (cell.mined()) {
            return false;
        }
        BlockState now = level.getBlockState(pos);
        if (cell.placed()) {
            return now != cell.before || !BlockSnapshots.sameKey(cell.beforeKey, BlockSnapshots.key(level, pos));
        }
        return now == cell.after
            && now != cell.before
            && cell.beforeEntity == null
            && level.getBlockEntity(pos) == null
            && (cell.before.canBeReplaced() || cell.before.getBlock() == now.getBlock());
    }

    private static List<BlockPos> restored(ServerLevel level, PlacementRecord record) {
        List<BlockPos> cells = new ArrayList<>();
        for (Map.Entry<BlockPos, PlacementRecord.Cell> entry : record.cells.entrySet()) {
            if (restorable(level, entry.getKey(), entry.getValue())) {
                cells.add(entry.getKey());
            }
        }
        return cells;
    }

    private static boolean inTheWay(ServerLevel level, PlacementRecord record, List<BlockPos> restored) {
        for (BlockPos pos : restored) {
            VoxelShape shape = record.cells.get(pos).before.getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            AABB box = shape.bounds().move(pos);
            if (!level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity.isAlive() && !entity.isSpectator()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean permitted(ServerLevel level, ServerPlayer player, List<BlockPos> restored) {
        for (BlockPos pos : restored) {
            BlockState state = level.getBlockState(pos);
            BlockEntity entity = level.getBlockEntity(pos);
            if (!level.mayInteract(player, pos)
                || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())
                || !PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, entity)) {
                return false;
            }
        }
        return true;
    }

    private static void apply(ServerLevel level, ServerPlayer player, PlacementRecord record) {
        List<BlockPos> restored = restored(level, record);
        Set<BlockPos> ends = new LinkedHashSet<>();
        for (Map.Entry<BlockPos, Set<BlockPos>> entry : record.newCurves.entrySet()) {
            ends.add(entry.getKey());
            ends.addAll(entry.getValue());
        }
        for (Map.Entry<BlockPos, Set<BlockPos>> entry : record.newCurves.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!(level.getBlockEntity(pos) instanceof TrackBlockEntity track)) {
                continue;
            }
            for (BlockPos target : entry.getValue()) {
                if (!track.getConnections().containsKey(target)) {
                    continue;
                }
                track.removeConnection(target);
                if (level.getBlockEntity(target) instanceof TrackBlockEntity other) {
                    other.removeConnection(pos);
                }
            }
            TrackPropagator.onRailRemoved(level, pos, level.getBlockState(pos));
        }
        for (int pass = 0; pass < 3; pass++) {
            for (BlockPos pos : restored) {
                PlacementRecord.Cell cell = record.cells.get(pos);
                if (pass != order(level, pos, cell)) {
                    continue;
                }
                BlockState now = level.getBlockState(pos);
                if (now == cell.before) {
                    continue;
                }
                level.setBlock(pos, cell.before, Block.UPDATE_ALL);
            }
        }
        for (BlockPos pos : restored) {
            PlacementRecord.Cell cell = record.cells.get(pos);
            if (cell.beforeEntity == null || BlockSnapshots.sameKey(cell.beforeKey, BlockSnapshots.key(level, pos))) {
                continue;
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), cell.beforeEntity));
            entity.setChanged();
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
        for (BlockPos pos : restored) {
            PlacementRecord.Cell cell = record.cells.get(pos);
            if (cell.placed() && cell.beforeEntity == null && level.getBlockEntity(pos) != null && level.getBlockState(pos) == cell.before) {
                level.removeBlockEntity(pos);
            }
        }
        int off = 0;
        for (BlockPos pos : restored) {
            PlacementRecord.Cell cell = record.cells.get(pos);
            if (cell.placed() && level.getBlockState(pos) != cell.before) {
                level.setBlock(pos, cell.before, Block.UPDATE_ALL);
                if (level.getBlockState(pos) != cell.before) {
                    off++;
                }
            }
        }
        for (BlockPos pos : ends) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ITrackBlock) {
                TrackPropagator.onRailAdded(level, pos, state);
            }
        }
        if (off > 0) {
            Trackwright.LOGGER.warn("Undoing a track placement for {} left {} blocks different from before", player.getScoreboardName(), off);
        }
        for (ItemStack stack : record.charged) {
            player.getInventory().placeItemBackInInventory(stack.copy());
        }
        returnSelection(level, player, record);
    }

    private static int order(ServerLevel level, BlockPos pos, PlacementRecord.Cell cell) {
        if (!cell.placed()) {
            return 2;
        }
        BlockState now = level.getBlockState(pos);
        return now.getBlock() instanceof ITrackBlock || BlockSnapshots.fake(now) ? 0 : 1;
    }

    private static void returnSelection(ServerLevel level, ServerPlayer player, PlacementRecord record) {
        if (record.from == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(record.trackItem) || held.has(AllDataComponents.TRACK_CONNECTING_FROM)) {
            return;
        }
        if (level.getBlockState(record.from.pos()).getBlock() instanceof ITrackBlock) {
            held.set(AllDataComponents.TRACK_CONNECTING_FROM, record.from);
        }
    }
}
