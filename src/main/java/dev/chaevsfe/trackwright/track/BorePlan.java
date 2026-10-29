package dev.chaevsfe.trackwright.track;

import com.simibubi.create.content.trains.track.ITrackBlock;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record BorePlan(
    List<BlockPos> cells,
    List<BlockPos> trackCells,
    List<BlockPos> bodyCells,
    List<BlockPos> bandCells,
    BoreProfile profile,
    int wear,
    int usesLeft,
    boolean creative,
    BlockMiner.@Nullable Problem problem,
    boolean guessed
) {
    public BorePlan {
        cells = List.copyOf(cells);
        trackCells = List.copyOf(trackCells);
        bodyCells = List.copyOf(bodyCells);
        bandCells = List.copyOf(bandCells);
    }

    public BorePlan withProblem(BlockMiner.Problem found) {
        return new BorePlan(cells, trackCells, bodyCells, bandCells, profile, wear, usesLeft, creative, found, guessed);
    }

    public List<BlockPos> pathCells() {
        if (bodyCells.isEmpty()) {
            return trackCells;
        }
        List<BlockPos> path = new ArrayList<>(trackCells);
        path.addAll(bodyCells);
        return path;
    }

    public static BorePlan of(Level level, Player player, List<BlockPos> path, Set<BlockPos> blocked, Set<BlockPos> body,
                              BoreBand.Line line, BoreProfile profile, @Nullable BlockPos unloaded, boolean unseen,
                              TrackwrightSettings settings) {
        return of(level, player.getOffhandItem(), player.isCreative(), path, blocked, body, line, profile, unloaded, unseen, settings);
    }

    public static BorePlan of(Level level, ItemStack tool, boolean creative, List<BlockPos> path, Set<BlockPos> blocked, Set<BlockPos> body,
                              BoreBand.Line line, BoreProfile profile, @Nullable BlockPos unloaded, boolean unseen,
                              TrackwrightSettings settings) {
        BlockMiner.Rules rules = new BlockMiner.Rules(settings.boreLavaCheck(), settings.boreMaxHardness());
        Set<BlockPos> onPath = new HashSet<>(path);
        onPath.addAll(line.cells());
        Set<BlockPos> all = new LinkedHashSet<>();
        List<BlockPos> track = new ArrayList<>();
        List<BlockPos> bodyMined = new ArrayList<>();
        List<BlockPos> band = new ArrayList<>();
        BlockMiner.Problem problem = unloaded == null ? null
            : new BlockMiner.Problem(BlockMiner.Refusal.NOT_LOADED, unloaded.immutable(), Blocks.AIR.defaultBlockState());
        boolean guessed = unseen;
        Map<BlockPos, List<BlockPos>> beds = new HashMap<>();
        List<BlockPos> loose = new ArrayList<>();
        Set<BlockPos> seeds = new HashSet<>(line.cells());
        for (BlockPos base : path) {
            if (seeds.contains(base)) {
                continue;
            }
            BlockPos above = base.above();
            if (seeds.contains(above)) {
                beds.computeIfAbsent(above, key -> new ArrayList<>()).add(base);
            } else {
                loose.add(base);
            }
        }
        Set<BlockPos> blockedPath = new HashSet<>(path);
        blockedPath.retainAll(blocked);
        for (Map.Entry<BlockPos, List<BlockPos>> entry : BoreBand.sides(line, profile).entrySet()) {
            BlockPos seed = entry.getKey();
            List<BlockPos> lineCells = new ArrayList<>();
            lineCells.add(seed);
            lineCells.addAll(beds.getOrDefault(seed, List.of()));
            for (BlockPos cell : lineCells) {
                if (blockedPath.contains(cell) && all.add(cell)) {
                    (body.contains(cell) ? bodyMined : track).add(cell);
                }
            }
            List<BlockPos> columns = new ArrayList<>();
            columns.add(seed);
            columns.addAll(entry.getValue());
            for (BlockPos column : columns) {
                for (int h = column.equals(seed) ? 1 : 0; h <= profile.up(); h++) {
                    BlockPos pos = column.above(h);
                    if (onPath.contains(pos) || all.contains(pos)) {
                        continue;
                    }
                    if (!level.isLoaded(pos)) {
                        if (level.isClientSide()) {
                            guessed = true;
                        } else if (problem == null) {
                            problem = new BlockMiner.Problem(BlockMiner.Refusal.NOT_LOADED, pos.immutable(), Blocks.AIR.defaultBlockState());
                        }
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (BlockMiner.needsMining(state) && (column.equals(seed) || !standsBeside(level, pos, state))) {
                        BlockPos cell = pos.immutable();
                        band.add(cell);
                        all.add(cell);
                    }
                }
            }
        }
        for (BlockPos cell : loose) {
            if (blockedPath.contains(cell) && all.add(cell)) {
                (body.contains(cell) ? bodyMined : track).add(cell);
            }
        }
        int left = creative ? -1 : BlockMiner.usesLeft(tool);
        int wear = 0;
        for (BlockPos pos : all) {
            BlockState state = level.getBlockState(pos);
            if (problem == null) {
                problem = state.getBlock() instanceof ITrackBlock
                    ? new BlockMiner.Problem(BlockMiner.Refusal.IMMUNE, pos, state)
                    : BlockMiner.checkBlock(level, pos, state, tool, creative, rules);
            }
            if (!creative) {
                wear += BlockMiner.wear(level, tool, pos, state);
                if (problem == null && left >= 0 && wear > left) {
                    problem = new BlockMiner.Problem(BlockMiner.Refusal.TOOL_WORN, pos, state);
                }
            }
        }
        if (problem == null) {
            problem = BlockMiner.checkSurroundings(level, all, rules);
        }
        return new BorePlan(new ArrayList<>(all), track, bodyMined, band, profile, wear, left, creative, problem, guessed);
    }

    private static boolean standsBeside(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof ITrackBlock || state.hasBlockEntity() || state.is(BlockMiner.IMMUNE)
            || state.getDestroySpeed(level, pos) < 0;
    }
}
