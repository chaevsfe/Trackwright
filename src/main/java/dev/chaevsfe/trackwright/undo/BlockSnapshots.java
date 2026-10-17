package dev.chaevsfe.trackwright.undo;

import com.zurrtum.create.content.trains.track.FakeTrackBlock;
import com.zurrtum.create.content.trains.track.FakeTrackBlockEntity;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

final class BlockSnapshots {
    private static final String CONNECTIONS = "Connections";
    private static final String SMOOTHING = "Smoothing";

    private BlockSnapshots() {
    }

    static @Nullable CompoundTag full(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity == null ? null : entity.saveWithFullMetadata(level.registryAccess());
    }

    static @Nullable CompoundTag key(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null || entity instanceof FakeTrackBlockEntity) {
            return null;
        }
        CompoundTag tag = entity.saveWithoutMetadata(level.registryAccess());
        if (entity instanceof TrackBlockEntity) {
            tag.remove(SMOOTHING);
            if (tag.get(CONNECTIONS) instanceof ListTag list) {
                for (Tag element : list) {
                    if (element instanceof CompoundTag connection) {
                        connection.remove(SMOOTHING);
                    }
                }
            }
        }
        return tag;
    }

    static Set<BlockPos> connections(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TrackBlockEntity track) {
            return new HashSet<>(track.getConnections().keySet());
        }
        return Set.of();
    }

    static boolean fake(BlockState state) {
        return state.getBlock() instanceof FakeTrackBlock;
    }

    static boolean sameKey(@Nullable CompoundTag expected, @Nullable CompoundTag found) {
        return expected == null ? found == null : expected.equals(found);
    }
}
