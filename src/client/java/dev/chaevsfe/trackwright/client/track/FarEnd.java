package dev.chaevsfe.trackwright.client.track;

import com.zurrtum.create.client.flywheel.lib.visualization.VisualizationHelper;
import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class FarEnd {
    private static final Set<BlockPos> STALE = new HashSet<>();
    private static final Set<BlockPos> LOADED = new HashSet<>();
    private static final Set<BlockPos> VISUALS = ConcurrentHashMap.newKeySet();
    private static final Queue<BlockPos> VISUALS_CHANGED = new ConcurrentLinkedQueue<>();
    private static final Long2LongOpenHashMap EXTRACTED = new Long2LongOpenHashMap();
    private static long frame;

    private FarEnd() {
    }

    public static boolean firstEndUnloaded(Level level, BezierConnection connection) {
        if (level != Minecraft.getInstance().level) {
            return false;
        }
        BlockPos first = connection.getKey();
        return !level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(first.getX()), SectionPos.blockToSectionCoord(first.getZ()));
    }

    public static boolean firstEndWithoutVisual(Level level, BezierConnection connection) {
        if (level != Minecraft.getInstance().level) {
            return false;
        }
        return firstEndUnloaded(level, connection) || !VISUALS.contains(connection.getKey());
    }

    public static boolean firstEndHidden(Level level, BezierConnection connection) {
        if (level != Minecraft.getInstance().level) {
            return false;
        }
        if (firstEndUnloaded(level, connection)) {
            return true;
        }
        return EXTRACTED.getOrDefault(connection.getKey().asLong(), Long.MIN_VALUE) < frame - 1;
    }

    public static void beginFrame() {
        frame++;
    }

    public static void extracted(TrackBlockEntity track) {
        if (track.getLevel() == Minecraft.getInstance().level) {
            EXTRACTED.put(track.getBlockPos().asLong(), frame);
        }
    }

    public static void visualCreated(TrackBlockEntity track) {
        if (track.getLevel() == Minecraft.getInstance().level) {
            BlockPos pos = track.getBlockPos().immutable();
            VISUALS.add(pos);
            VISUALS_CHANGED.add(pos);
        }
    }

    public static void visualDeleted(TrackBlockEntity track) {
        if (track.getLevel() == Minecraft.getInstance().level) {
            BlockPos pos = track.getBlockPos().immutable();
            VISUALS.remove(pos);
            VISUALS_CHANGED.add(pos);
        }
    }

    public static void endUnloaded(BlockEntity blockEntity) {
        if (blockEntity instanceof TrackBlockEntity track) {
            STALE.addAll(track.getConnections().keySet());
            EXTRACTED.remove(track.getBlockPos().asLong());
        }
    }

    public static void endLoaded(BlockEntity blockEntity) {
        if (blockEntity instanceof TrackBlockEntity) {
            LOADED.add(blockEntity.getBlockPos());
        }
    }

    public static void refresh(Minecraft mc) {
        BlockPos changed;
        while ((changed = VISUALS_CHANGED.poll()) != null) {
            LOADED.add(changed);
        }
        if (STALE.isEmpty() && LOADED.isEmpty()) {
            return;
        }
        ClientLevel level = mc.level;
        if (level != null) {
            for (BlockPos pos : LOADED) {
                if (level.getBlockEntity(pos) instanceof TrackBlockEntity track) {
                    STALE.addAll(track.getConnections().keySet());
                }
            }
            for (BlockPos pos : STALE) {
                if (level.getBlockEntity(pos) instanceof TrackBlockEntity track) {
                    VisualizationHelper.queueUpdate(track);
                }
            }
        }
        STALE.clear();
        LOADED.clear();
    }

    public static void reset() {
        STALE.clear();
        LOADED.clear();
        VISUALS.clear();
        VISUALS_CHANGED.clear();
        EXTRACTED.clear();
    }
}
