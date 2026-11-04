package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.content.trains.track.ITrackBlock;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.markers.Markers;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    boolean guessed,
    List<BlockPos> keptCells
) {
    public BorePlan {
        cells = List.copyOf(cells);
        trackCells = List.copyOf(trackCells);
        bodyCells = List.copyOf(bodyCells);
        bandCells = List.copyOf(bandCells);
        keptCells = List.copyOf(keptCells);
    }

    public BorePlan withProblem(BlockMiner.Problem found) {
        return new BorePlan(cells, trackCells, bodyCells, bandCells, profile, wear, usesLeft, creative, found, guessed, keptCells);
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
        return of(level, tool, creative, path, blocked, body, line, profile, unloaded, unseen, settings, false);
    }

    public static BorePlan of(Level level, ItemStack tool, boolean creative, List<BlockPos> path, Set<BlockPos> blocked, Set<BlockPos> body,
                              BoreBand.Line line, BoreProfile profile, @Nullable BlockPos unloaded, boolean unseen,
                              TrackwrightSettings settings, boolean clearing) {
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
        Set<BlockPos> side = new HashSet<>();
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
                    if (BlockMiner.needsMining(state) && (clearing || column.equals(seed) || !standsBeside(level, pos, state))) {
                        BlockPos cell = pos.immutable();
                        band.add(cell);
                        all.add(cell);
                        if (!column.equals(seed)) {
                            side.add(cell);
                        }
                    }
                }
            }
        }
        for (BlockPos cell : loose) {
            if (blockedPath.contains(cell) && all.add(cell)) {
                (body.contains(cell) ? bodyMined : track).add(cell);
            }
        }
        List<BlockPos> kept = new ArrayList<>();
        if (clearing) {
            Set<BlockPos> skipped = new HashSet<>();
            for (BlockPos pos : all) {
                if (!Markers.isKnown(level, pos)) {
                    guessed = true;
                }
                if (BlockMiner.kept(level, pos, level.getBlockState(pos))) {
                    skipped.add(pos);
                    kept.add(pos);
                }
            }
            all.removeAll(skipped);
            track.removeAll(skipped);
            bodyMined.removeAll(skipped);
            band.removeAll(skipped);
        }
        Held held = holdUp(level, all, side, clearing, kept, track, bodyMined, band);
        guessed |= held.guessed();
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
            problem = held.problem();
        }
        if (problem == null) {
            problem = BlockMiner.checkSurroundings(level, all, rules);
        }
        return new BorePlan(new ArrayList<>(all), track, bodyMined, band, profile, wear, left, creative, problem, guessed, kept);
    }

    private record Held(BlockMiner.@Nullable Problem problem, boolean guessed) {
    }

    private static Held holdUp(Level level, Set<BlockPos> all, Set<BlockPos> side, boolean clearing, List<BlockPos> kept,
                               List<BlockPos> track, List<BlockPos> bodyMined, List<BlockPos> band) {
        Map<BlockPos, BlockState> leaning = new LinkedHashMap<>();
        Set<BlockPos> seen = new HashSet<>();
        boolean guessed = false;
        MinedView first = new MinedView(level, all);
        for (BlockPos pos : all) {
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (all.contains(next) || !seen.add(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.isAir() || !state.canSurvive(level, next)) {
                    continue;
                }
                if (BlockMiner.kept(level, next, state)) {
                    leaning.put(next, state);
                } else if (clearing && !Markers.isKnown(level, next) && !state.canSurvive(first, next)) {
                    guessed = true;
                }
            }
        }
        boolean changed = !leaning.isEmpty();
        while (changed) {
            changed = false;
            MinedView view = new MinedView(level, all);
            for (Map.Entry<BlockPos, BlockState> entry : leaning.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();
                if (state.canSurvive(view, pos)) {
                    continue;
                }
                List<BlockPos> under = new ArrayList<>();
                for (Direction direction : Direction.values()) {
                    BlockPos next = pos.relative(direction);
                    if (all.contains(next)) {
                        under.add(next.immutable());
                    }
                }
                if (under.isEmpty() || !state.canSurvive(view.restoring(under), pos)) {
                    continue;
                }
                List<BlockPos> movable = clearing ? under : under.stream().filter(side::contains).toList();
                List<BlockPos> stand = null;
                for (BlockPos cell : movable) {
                    if (state.canSurvive(view.restoring(List.of(cell)), pos)) {
                        stand = List.of(cell);
                        break;
                    }
                }
                if (stand == null && !movable.isEmpty() && state.canSurvive(view.restoring(movable), pos)) {
                    stand = movable;
                }
                if (stand == null) {
                    if (clearing) {
                        stand = under;
                    } else {
                        return new Held(new BlockMiner.Problem(leaningRefusal(level, pos, state), pos, state), guessed);
                    }
                }
                all.removeAll(stand);
                track.removeAll(stand);
                bodyMined.removeAll(stand);
                band.removeAll(stand);
                if (clearing) {
                    kept.addAll(stand);
                }
                changed = true;
            }
        }
        return new Held(null, guessed);
    }

    private static BlockMiner.Refusal leaningRefusal(Level level, BlockPos pos, BlockState state) {
        if (Markers.isProtected(level, pos)) {
            return BlockMiner.Refusal.MARKED;
        }
        if (state.hasBlockEntity()) {
            return BlockMiner.Refusal.BLOCK_ENTITY;
        }
        return state.getDestroySpeed(level, pos) < 0 ? BlockMiner.Refusal.UNBREAKABLE : BlockMiner.Refusal.IMMUNE;
    }

    private static boolean standsBeside(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof ITrackBlock || state.hasBlockEntity() || state.is(BlockMiner.IMMUNE)
            || state.getDestroySpeed(level, pos) < 0 || Markers.isProtected(level, pos);
    }
}
