package dev.chaevsfe.trackwright.track;

import com.zurrtum.create.content.trains.track.ITrackBlock;
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
import java.util.List;
import java.util.Set;

public record BorePlan(
    List<BlockPos> cells,
    List<BlockPos> trackCells,
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
        headroomCells = List.copyOf(headroomCells);
    }

    public BorePlan withProblem(BlockMiner.Problem found) {
        return new BorePlan(cells, trackCells, headroomCells, headroom, wear, usesLeft, creative, found, guessed);
    }

    public static BorePlan of(Level level, Player player, StraightConnections.Line line, int cells, Set<BlockPos> blocked,
                              @Nullable BlockPos unloaded, boolean unseen, TrackwrightSettings settings) {
        boolean creative = player.isCreative();
        ItemStack tool = player.getOffhandItem();
        BlockMiner.Rules rules = new BlockMiner.Rules(settings.boreLavaCheck(), settings.boreMaxHardness());
        int headroom = settings.boreHeadroom();
        List<BlockPos> all = new ArrayList<>();
        List<BlockPos> track = new ArrayList<>();
        List<BlockPos> above = new ArrayList<>();
        BlockMiner.Problem problem = unloaded == null ? null
            : new BlockMiner.Problem(BlockMiner.Refusal.NOT_LOADED, unloaded.immutable(), Blocks.AIR.defaultBlockState());
        boolean guessed = unseen;
        for (int i = 1; i < cells - 1; i++) {
            BlockPos base = line.at(i);
            if (blocked.contains(base)) {
                track.add(base);
                all.add(base);
            }
            for (int h = 1; h <= headroom; h++) {
                BlockPos pos = base.above(h);
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
        return new BorePlan(all, track, above, headroom, wear, left, creative, problem, guessed);
    }
}
