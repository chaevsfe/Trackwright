package dev.chaevsfe.trackwright.markers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

public final class MarkerSet {
    public static final int VERSION = 1;
    public static final MarkerSet EMPTY = new MarkerSet(new int[0]);
    public static final Codec<MarkerSet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("version", VERSION).forGetter(set -> VERSION),
        Codec.INT_STREAM.optionalFieldOf("cells").forGetter(set -> Optional.of(IntStream.of(set.cells)))
    ).apply(instance, (version, cells) -> of(cells.map(IntStream::toArray).orElseGet(() -> new int[0]))));

    private final int[] cells;
    private final IntOpenHashSet lookup;

    private MarkerSet(int[] sortedUnique) {
        this.cells = sortedUnique;
        this.lookup = new IntOpenHashSet(sortedUnique);
    }

    public static MarkerSet of(int[] cells) {
        if (cells.length == 0) {
            return EMPTY;
        }
        int[] copy = cells.clone();
        IntArrays.quickSort(copy);
        int size = 1;
        for (int i = 1; i < copy.length; i++) {
            if (copy[i] != copy[size - 1]) {
                copy[size++] = copy[i];
            }
        }
        return new MarkerSet(size == copy.length ? copy : Arrays.copyOf(copy, size));
    }

    public static int cell(BlockPos pos) {
        return (pos.getY() << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
    }

    public static BlockPos pos(ChunkPos chunk, int cell) {
        return new BlockPos(chunk.getMinBlockX() + (cell & 15), cell >> 8, chunk.getMinBlockZ() + ((cell >> 4) & 15));
    }

    public boolean contains(int cell) {
        return lookup.contains(cell);
    }

    public boolean isEmpty() {
        return cells.length == 0;
    }

    public int size() {
        return cells.length;
    }

    public int[] cells() {
        return cells.clone();
    }

    public MarkerSet with(IntOpenHashSet added, IntOpenHashSet removed) {
        IntOpenHashSet next = new IntOpenHashSet(lookup);
        next.addAll(added);
        next.removeAll(removed);
        return of(next.toIntArray());
    }
}
