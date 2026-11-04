package dev.chaevsfe.trackwright.markers;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.chaevsfe.trackwright.config.MarkerMode;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public final class MarkerCommands {
    public static final int COUNT_RADIUS = 32;
    public static final int COUNT_CHUNK_CAP = 1024;

    private MarkerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trackwright")
            .then(Commands.literal("mark")
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                    .executes(context -> change(context, true, false))
                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .executes(context -> change(context, true, true)))))
            .then(Commands.literal("unmark")
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                    .executes(context -> change(context, false, false))
                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .executes(context -> change(context, false, true)))))
            .then(Commands.literal("marks")
                .executes(MarkerCommands::countAround)
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .executes(MarkerCommands::countBox)))));
    }

    private static int change(CommandContext<CommandSourceStack> context, boolean mark, boolean box) {
        CommandSourceStack source = context.getSource();
        MarkerMode mode = ServerConfig.get().unbreakableMarkers();
        Component refused = Markers.refusal(mode, mode.allows(source));
        if (refused != null) {
            source.sendFailure(refused);
            return 0;
        }
        BlockPos from = BlockPosArgument.getBlockPos(context, "from");
        BlockPos to = box ? BlockPosArgument.getBlockPos(context, "to") : from;
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);
        long volume = (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        if (volume > Markers.BOX_CAP) {
            source.sendFailure(MarkerMessages.refusal(MarkerMessages.BOX_TOO_BIG, volume, Markers.BOX_CAP));
            return 0;
        }
        ServerLevel level = source.getLevel();
        ServerPlayer player = source.getPlayer();
        if (player != null && BlockMiner.symmetryWandInHotbar(player)) {
            source.sendFailure(MarkerMessages.refusal(MarkerMessages.SYMMETRY));
            return 0;
        }
        List<BlockPos> cells = new ArrayList<>();
        int skipped = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!Markers.isLoaded(level, pos)) {
                skipped++;
                continue;
            }
            if (mark ? level.getBlockState(pos).isAir() : !Markers.isMarked(level, pos)) {
                continue;
            }
            if (player != null && !Markers.mayChange(level, player, pos)) {
                skipped++;
                continue;
            }
            cells.add(pos.immutable());
        }
        Markers.change(level, cells, mark);
        int done = cells.size();
        int skippedCount = skipped;
        source.sendSuccess(() -> mark ? MarkerMessages.counted(MarkerMessages.DONE_MARK, MarkerMessages.DONE_MARK_ONE, done, skippedCount)
            : MarkerMessages.counted(MarkerMessages.DONE_UNMARK, MarkerMessages.DONE_UNMARK_ONE, done, skippedCount), true);
        return done;
    }

    private static int countAround(CommandContext<CommandSourceStack> context) {
        BlockPos center = BlockPos.containing(context.getSource().getPosition());
        return count(context.getSource(), center.offset(-COUNT_RADIUS, -COUNT_RADIUS, -COUNT_RADIUS), center.offset(COUNT_RADIUS, COUNT_RADIUS, COUNT_RADIUS));
    }

    private static int countBox(CommandContext<CommandSourceStack> context) {
        return count(context.getSource(), BlockPosArgument.getBlockPos(context, "from"), BlockPosArgument.getBlockPos(context, "to"));
    }

    private static int count(CommandSourceStack source, BlockPos from, BlockPos to) {
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);
        int minChunkX = min.getX() >> 4;
        int minChunkZ = min.getZ() >> 4;
        int maxChunkX = max.getX() >> 4;
        int maxChunkZ = max.getZ() >> 4;
        long chunks = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (chunks > COUNT_CHUNK_CAP) {
            source.sendFailure(MarkerMessages.refusal(MarkerMessages.COUNT_TOO_BIG, chunks, COUNT_CHUNK_CAP));
            return 0;
        }
        ServerLevel level = source.getLevel();
        int found = 0;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                MarkerSet set = Markers.setAt(level, chunkX, chunkZ);
                if (set == null) {
                    continue;
                }
                ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                for (int cell : set.cells()) {
                    BlockPos pos = MarkerSet.pos(chunk, cell);
                    if (pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                        && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ()) {
                        found++;
                    }
                }
            }
        }
        int total = found;
        boolean on = ServerConfig.get().unbreakableMarkers().enabled();
        source.sendSuccess(() -> on ? MarkerMessages.counted(MarkerMessages.COUNT, MarkerMessages.COUNT_ONE, total)
            : MarkerMessages.counted(MarkerMessages.COUNT_OFF, MarkerMessages.COUNT_OFF_ONE, total), false);
        return total;
    }
}
