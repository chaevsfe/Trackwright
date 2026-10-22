package dev.chaevsfe.trackwright.track;

import com.simibubi.create.content.trains.track.ITrackBlock;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record BorePlan(
    List<BlockPos> cells,
    List<BlockPos> trackCells,
    List<BlockPos> bodyCells,
    List<BlockPos> headroomCells,
    int headroom,
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
        headroomCells = List.copyOf(headroomCells);
    }

    public BorePlan withProblem(BlockMiner.Problem found) {
        return new BorePlan(cells, trackCells, bodyCells, headroomCells, headroom, wear, usesLeft, creative, found, guessed);
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
                              @Nullable BlockPos unloaded, boolean unseen, TrackwrightSettings settings) {
        return of(level, player.getOffhandItem(), player.isCreative(), path, blocked, body, unloaded, unseen, settings);
    }

    public static BorePlan of(Level level, ItemStack tool, boolean creative, List<BlockPos> path, Set<BlockPos> blocked, Set<BlockPos> body,
                              @Nullable BlockPos unloaded, boolean unseen, TrackwrightSettings settings) {
        BlockMiner.Rules rules = new BlockMiner.Rules(settings.boreLavaCheck(), settings.boreMaxHardness());
        int headroom = settings.boreHeadroom();
        Set<BlockPos> onPath = new HashSet<>(path);
        Set<BlockPos> all = new LinkedHashSet<>();
        List<BlockPos> track = new ArrayList<>();
        List<BlockPos> bodyMined = new ArrayList<>();
        List<BlockPos> above = new ArrayList<>();
        BlockMiner.Problem problem = unloaded == null ? null
            : new BlockMiner.Problem(BlockMiner.Refusal.NOT_LOADED, unloaded.immutable(), Blocks.AIR.defaultBlockState());
        boolean guessed = unseen;
        for (BlockPos base : path) {
            if (blocked.contains(base) && all.add(base)) {
                (body.contains(base) ? bodyMined : track).add(base);
            }
            for (int h = 1; h <= headroom; h++) {
                BlockPos pos = base.above(h);
                if (onPath.contains(pos) || all.contains(pos)) {
                    continue;
                }
                if (!level.isLoaded(pos)) {
                    if (level.isClientSide()) {
                        guessed = true;
                    } else if (problem == null) {
                        problem = new BlockMiner.Problem(BlockMiner.Refusal.NOT_LOADED, pos, Blocks.AIR.defaultBlockState());
                    }
                    continue;
                }
                if (BlockMiner.needsMining(level.getBlockState(pos))) {
                    above.add(pos);
                    all.add(pos);
                }
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
        return new BorePlan(new ArrayList<>(all), track, bodyMined, above, headroom, wear, left, creative, problem, guessed);
    }
}
