package dev.chaevsfe.trackwright.actors;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.chaevsfe.trackwright.TagReads;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class TrainActors {
    public static final TagKey<Block> ROLLER_CLEARS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Trackwright.MOD_ID, "roller_clears"));
    public static final String SCROLL_VALUE = "ScrollValue";
    public static final int TUNNEL_PAVE = 0;

    private TrainActors() {
    }

    public static boolean onTrain(MovementContext context) {
        return context.contraption instanceof CarriageContraption && context.world != null && !context.world.isClientSide();
    }

    public static boolean canBreak(BlockBreakingMovementBehaviour behaviour, MovementContext context, Level level, BlockPos pos, BlockState state, boolean original) {
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
            && context.blockEntityData != null && TagReads.intOr(context.blockEntityData, SCROLL_VALUE, TUNNEL_PAVE) == TUNNEL_PAVE;
    }

    public static boolean leavesFluid(MovementContext context, Level level, BlockPos pos, BlockState toPlace) {
        if (!onTrain(context) || !ServerConfig.get().rollersIgnoreFluids()) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        return !existing.getFluidState().isEmpty() && !existing.is(toPlace.getBlock());
    }

    public static boolean isTrackEquipment(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof ITrackBlock) {
            return true;
        }
        return BlockEntityBehaviour.get(level, pos, TrackTargetingBehaviour.TYPE) != null;
    }

    private static boolean rollerMayClear(Level level, BlockPos pos, BlockState state) {
        if (!state.is(ROLLER_CLEARS) || state.is(AllBlockTags.TRACKS.tag)) {
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
