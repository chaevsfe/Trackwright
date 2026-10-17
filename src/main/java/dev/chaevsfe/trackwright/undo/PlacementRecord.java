package dev.chaevsfe.trackwright.undo;

import com.zurrtum.create.infrastructure.component.ConnectingFrom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class PlacementRecord {
    enum Kind {
        NEIGHBOUR,
        WRITTEN,
        MINED
    }

    static final class Cell {
        Kind kind;
        final BlockState before;
        final @Nullable CompoundTag beforeEntity;
        final @Nullable CompoundTag beforeKey;
        final Set<BlockPos> beforeConnections;
        BlockState after;
        @Nullable CompoundTag afterKey;
        Set<BlockPos> afterConnections = Set.of();

        Cell(Kind kind, BlockState before, @Nullable CompoundTag beforeEntity, @Nullable CompoundTag beforeKey, Set<BlockPos> beforeConnections) {
            this.kind = kind;
            this.before = before;
            this.beforeEntity = beforeEntity;
            this.beforeKey = beforeKey;
            this.beforeConnections = beforeConnections;
            this.after = before;
        }

        boolean placed() {
            return kind != Kind.NEIGHBOUR;
        }
    }

    final ResourceKey<Level> dimension;
    final int placedTick;
    final long placedGameTime;
    final Map<BlockPos, Cell> cells;
    final Set<BlockPos> trackCells;
    final Set<BlockPos> watched;
    final Map<BlockPos, Set<BlockPos>> newCurves;
    final List<ItemStack> charged;
    final List<ItemStack> drops;
    final int experience;
    final ItemStack toolBefore;
    final ItemStack toolAfter;
    final boolean bored;
    final @Nullable ConnectingFrom from;
    final Item trackItem;
    final @Nullable String refusal;
    boolean settled;
    boolean changed;

    PlacementRecord(
        ResourceKey<Level> dimension,
        int placedTick,
        long placedGameTime,
        Map<BlockPos, Cell> cells,
        Set<BlockPos> trackCells,
        Set<BlockPos> watched,
        Map<BlockPos, Set<BlockPos>> newCurves,
        List<ItemStack> charged,
        List<ItemStack> drops,
        int experience,
        ItemStack toolBefore,
        ItemStack toolAfter,
        boolean bored,
        @Nullable ConnectingFrom from,
        Item trackItem,
        @Nullable String refusal
    ) {
        this.dimension = dimension;
        this.placedTick = placedTick;
        this.placedGameTime = placedGameTime;
        this.cells = cells;
        this.trackCells = trackCells;
        this.watched = watched;
        this.newCurves = newCurves;
        this.charged = charged;
        this.drops = drops;
        this.experience = experience;
        this.toolBefore = toolBefore;
        this.toolAfter = toolAfter;
        this.bored = bored;
        this.from = from;
        this.trackItem = trackItem;
        this.refusal = refusal;
    }
}
