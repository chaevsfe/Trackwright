package dev.chaevsfe.trackwright.undo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class UndoCommand {
    private UndoCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trackwright")
            .then(Commands.literal("undo").executes(UndoCommand::undo)));
    }

    private static int undo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(UndoMessages.refusal(UndoMessages.PLAYERS_ONLY));
            return 0;
        }
        TrackUndo.Result result = TrackUndo.undoAndTell(player);
        if (result.done()) {
            source.sendSuccess(result::message, false);
            return 1;
        }
        source.sendFailure(result.message());
        return 0;
    }
}
