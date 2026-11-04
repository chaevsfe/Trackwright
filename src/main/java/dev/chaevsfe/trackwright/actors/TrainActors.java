package dev.chaevsfe.trackwright.actors;

import com.zurrtum.create.AllBlockTags;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import com.zurrtum.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.zurrtum.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.zurrtum.create.content.trains.entity.CarriageContraption;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackTargetingBehaviour;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.markers.Markers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class TrainActors {
    public static final TagKey<Block> ROLLER_CLEARS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "roller_clears"));
    public static final TagKey<Block> SEMAPHORE_POLES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("railways", "semaphore_poles"));
    public static final TagKey<Block> SEMAPHORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "semaphores"));
    private static final Direction[] POLE_SIDES = {Direction.UP, Direction.DOWN};
    private static final int POLE_REACH = 16;
    public static final String SCROLL_VALUE = "ScrollValue";
    public static final int TUNNEL_PAVE = 0;

    private TrainActors() {
    }

    public static boolean onTrain(MovementContext context) {
        return context.contraption instanceof CarriageContraption && context.world != null && !context.world.isClientSide();
    }

    public static boolean canBreak(BlockBreakingMovementBehaviour behaviour, MovementContext context, Level level, BlockPos pos, BlockState state, boolean original) {
        if (Markers.isProtected(level, pos)) {
            return false;
        }
        if (!onTrain(context)) {
            return original;
        }
        TrackwrightSettings settings = ServerConfig.get();
        if (original) {
            return !(settings.protectTrackEquipment() && isTrackEquipment(level, pos, state));
        }
        return behaviour instanceof RollerMovementBehaviour && settings.rollersClearPowderSnow() && rollerMayClear(level, pos, state)
            && !(settings.protectTrackEquipment() && isTrackEquipment(level, pos, state));
    }

    public static boolean skipsTunnelPaving(MovementContext context) {
        return onTrain(context) && ServerConfig.get().rollersSkipAirPaving()
            && context.blockEntityData != null && context.blockEntityData.getIntOr(SCROLL_VALUE, TUNNEL_PAVE) == TUNNEL_PAVE;
    }

    public static boolean leavesFluid(MovementContext context, Level level, BlockPos pos, BlockState toPlace) {
        if (!onTrain(context) || !ServerConfig.get().rollersIgnoreFluids()) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        return !existing.getFluidState().isEmpty() && !existing.is(toPlace.getBlock());
    }

    public static boolean isTrackEquipment(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof ITrackBlock || block instanceof IWrenchable || carriesSemaphore(level, pos, state)) {
            return true;
        }
        return BlockEntityBehaviour.get(level, pos, TrackTargetingBehaviour.TYPE) != null;
    }

    private static boolean carriesSemaphore(Level level, BlockPos pos, BlockState state) {
        if (!state.is(SEMAPHORE_POLES)) {
            return false;
        }
        for (Direction side : POLE_SIDES) {
            BlockPos.MutableBlockPos cursor = pos.mutable();
            for (int step = 0; step < POLE_REACH; step++) {
                BlockState next = level.getBlockState(cursor.move(side));
                if (next.is(SEMAPHORES)) {
                    return true;
                }
                if (!next.is(SEMAPHORE_POLES)) {
                    break;
                }
            }
        }
        return false;
    }

    private static boolean rollerMayClear(Level level, BlockPos pos, BlockState state) {
        if (!state.is(ROLLER_CLEARS) || state.is(AllBlockTags.TRACKS)) {
            return false;
        }
        for (Direction side : Direction.values()) {
            if (level.getBlockState(pos.relative(side)).is(BlockTags.PORTALS)) {
                return false;
            }
        }
        return BlockBreakingKineticBlockEntity.isBreakable(state, state.getDestroySpeed(level, pos));
    }
}
