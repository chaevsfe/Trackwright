package dev.chaevsfe.trackwright.client.actors;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class RollerTerrainSlot extends ValueBoxTransform {
    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 15.5, 5), angle(state), Direction.Axis.Y);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack poseStack) {
        TransformStack.of(poseStack).rotateYDegrees(angle(state)).rotateXDegrees(90);
    }

    @Override
    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
        Vec3 offset = getLocalOffset(level, pos, state);
        return offset != null && localHit.distanceTo(offset) < scale / 3;
    }

    private static float angle(BlockState state) {
        return AngleHelper.horizontalAngle(state.getValue(RollerBlock.FACING)) + 180;
    }
}
