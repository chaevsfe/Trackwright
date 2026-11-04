package dev.chaevsfe.trackwright.client.markers;

import com.simibubi.create.AllSpecialTextures;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.markers.MarkerMessages;
import dev.chaevsfe.trackwright.markers.MarkerSync;
import dev.chaevsfe.trackwright.markers.Markers;
import dev.chaevsfe.trackwright.network.MarkersPayload;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ClientMarkers {
    private static final String OUTLINE = "trackwright_markers";
    private static final int COLOR = 0xE8B830;
    private static final int SHOW_RADIUS = 32;
    private static final int REBUILD_DISTANCE = 8;

    private static final LongOpenHashSet CELLS = new LongOpenHashSet();
    private static @Nullable String dimension;
    private static boolean truncated = true;
    private static long center = ChunkPos.INVALID_CHUNK_POS;
    private static boolean dirty;
    private static @Nullable BlockPos builtAt;
    private static boolean warnedNewer;

    private ClientMarkers() {
    }

    public static void receive(MarkersPayload payload) {
        if (payload.version() > MarkersPayload.VERSION && !warnedNewer) {
            warnedNewer = true;
            Trackwright.LOGGER.info("This server's Trackwright sends marker format {} and this client reads format {}; only the fields both know are used",
                payload.version(), MarkersPayload.VERSION);
        }
        MarkersPayload.Snapshot snapshot = payload.snapshot();
        CELLS.clear();
        for (long cell : snapshot.cells()) {
            CELLS.add(cell);
        }
        dimension = snapshot.dimension();
        truncated = snapshot.truncated();
        center = snapshot.center();
        dirty = true;
    }

    public static boolean contains(Level level, BlockPos pos) {
        return dimension != null && dimension.equals(level.dimension().location().toString()) && CELLS.contains(pos.asLong());
    }

    public static boolean covers(Level level, BlockPos pos) {
        if (dimension == null || truncated || center == ChunkPos.INVALID_CHUNK_POS || !dimension.equals(level.dimension().location().toString())) {
            return false;
        }
        return Math.abs((pos.getX() >> 4) - ChunkPos.getX(center)) <= MarkerSync.RADIUS_CHUNKS
            && Math.abs((pos.getZ() >> 4) - ChunkPos.getZ(center)) <= MarkerSync.RADIUS_CHUNKS;
    }

    public static void reset() {
        CELLS.clear();
        dimension = null;
        truncated = true;
        center = ChunkPos.INVALID_CHUNK_POS;
        dirty = false;
        builtAt = null;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            if (dimension != null) {
                reset();
            }
            return;
        }
        if (dimension == null) {
            return;
        }
        if (!enabled() || !MarkerSync.wantsMarkers(player) || !dimension.equals(mc.level.dimension().location().toString())) {
            reset();
            return;
        }
        if (!Markers.isMarkerTool(player.getMainHandItem()) || CELLS.isEmpty()) {
            builtAt = null;
            return;
        }
        BlockPos here = player.blockPosition();
        Outliner outliner = Outliner.getInstance();
        boolean moved = builtAt == null || builtAt.distManhattan(here) > REBUILD_DISTANCE;
        if (dirty || moved || !outliner.getOutlines().containsKey(OUTLINE)) {
            List<BlockPos> shown = new ArrayList<>();
            long radius = (long) SHOW_RADIUS * SHOW_RADIUS;
            for (long cell : CELLS) {
                BlockPos pos = BlockPos.of(cell);
                if (pos.distSqr(here) <= radius) {
                    shown.add(pos);
                }
            }
            dirty = false;
            builtAt = here;
            if (shown.isEmpty()) {
                outliner.remove(OUTLINE);
                return;
            }
            outliner.showCluster(OUTLINE, shown)
                .colored(COLOR)
                .disableLineNormals()
                .lineWidth(1 / 32f)
                .withFaceTexture(AllSpecialTextures.CHECKERED);
            return;
        }
        outliner.keep(OUTLINE);
    }

    public static void tooltip(ItemStack stack, List<Component> lines) {
        if (enabled() && Markers.isMarkerTool(stack)) {
            lines.add(MarkerMessages.text(MarkerMessages.TOOLTIP).withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean enabled() {
        TrackwrightSettings settings = RemoteSettings.get();
        return settings != null && settings.unbreakableMarkers().enabled();
    }
}
