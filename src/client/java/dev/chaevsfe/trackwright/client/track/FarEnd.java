package dev.chaevsfe.trackwright.client.track;

import com.zurrtum.create.client.flywheel.lib.visualization.VisualizationHelper;
import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class FarEnd {
    private static final Set<BlockPos> STALE = new HashSet<>();

    private FarEnd() {
    }

    public static boolean firstEndUnloaded(Level level, BezierConnection connection) {
        BlockPos first = connection.getKey();
        return !level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(first.getX()), SectionPos.blockToSectionCoord(first.getZ()));
    }

    public static boolean firstEndHidden(Level level, BezierConnection connection, Vec3 camera, int viewDistance) {
        if (firstEndUnloaded(level, connection)) {
            return true;
        }
        BlockPos first = connection.getKey();
        if (!Vec3.atCenterOf(first).closerThan(camera, viewDistance)) {
            return true;
        }
        int sure = Minecraft.getInstance().options.getEffectiveRenderDistance() - 1;
        long dx = SectionPos.blockToSectionCoord(first.getX()) - SectionPos.posToSectionCoord(camera.x);
        long dz = SectionPos.blockToSectionCoord(first.getZ()) - SectionPos.posToSectionCoord(camera.z);
        return dx * dx + dz * dz >= (long) sure * sure;
    }

    public static void endChanged(BlockEntity blockEntity) {
        if (blockEntity instanceof TrackBlockEntity track) {
            STALE.addAll(track.getConnections().keySet());
        }
    }

    public static void refresh(Minecraft mc) {
        if (STALE.isEmpty()) {
            return;
        }
        ClientLevel level = mc.level;
        if (level != null) {
            for (BlockPos pos : STALE) {
                if (level.getBlockEntity(pos) instanceof TrackBlockEntity track) {
                    VisualizationHelper.queueUpdate(track);
                }
            }
        }
        STALE.clear();
    }

    public static void reset() {
        STALE.clear();
    }
}
