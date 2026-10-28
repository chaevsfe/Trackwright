package dev.chaevsfe.trackwright.actors;

import com.zurrtum.create.content.contraptions.actors.roller.RollerBlockEntity;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import dev.chaevsfe.trackwright.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class RollerRules {
    private RollerRules() {
    }

    public static boolean reshapes(MovementContext context) {
        return context.world != null && !context.world.isClientSide() && ServerConfig.get().rollerReshape()
            && RollerTerrainBehaviour.of(context.blockEntityData) == RollerTerrain.FILTER_ONLY;
    }

    public static boolean reshapeMayBreak(MovementContext context, BlockState state, BlockState paveWith, @Nullable BlockState paveWithAsSlab) {
        if (!reshapes(context)) {
            return true;
        }
        if (paveWith.isAir()) {
            return false;
        }
        return state.is(paveWith.getBlock()) || paveWithAsSlab != null && state.is(paveWithAsSlab.getBlock());
    }

    public static boolean reshapeRefusesPaving(MovementContext context, Level level, BlockPos pos, BlockState toPlace) {
        if (!reshapes(context)) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        return !existing.isAir() && !existing.is(toPlace.getBlock());
    }

    public static boolean skipsForGround(MovementContext context, RollerBlockEntity.RollingMode mode, BlockPos cell, int depth, BlockState paveWith) {
        if (!TrainActors.onTrain(context) || !ServerConfig.get().rollersNeedGround()) {
            return false;
        }
        Level level = context.world;
        if (mode == RollerBlockEntity.RollingMode.TUNNEL_PAVE) {
            return isAirOrLiquid(level.getBlockState(cell.below()));
        }
        int bottom = cell.getY() + depth - AllConfigs.server().kinetics.rollerFillDepth.get() - 1;
        BlockPos.MutableBlockPos below = cell.mutable();
        for (int y = cell.getY() - 1; y >= bottom; y--) {
            below.setY(y);
            if (!level.isLoaded(below) || endsCreateFill(level, below, paveWith)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAirOrLiquid(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() && state.canBeReplaced();
    }

    private static boolean endsCreateFill(Level level, BlockPos pos, BlockState paveWith) {
        BlockState state = level.getBlockState(pos);
        if (state.is(paveWith.getBlock()) || state.is(BlockTags.LEAVES) || state.canBeReplaced()) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty() || state.is(BlockTags.PORTALS);
    }
}
