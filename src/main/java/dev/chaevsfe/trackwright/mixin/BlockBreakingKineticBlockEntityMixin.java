package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zurrtum.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import dev.chaevsfe.trackwright.markers.Markers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBreakingKineticBlockEntity.class)
public abstract class BlockBreakingKineticBlockEntityMixin {
    @Shadow
    protected @Nullable BlockPos breakingPos;

    @ModifyReturnValue(method = "canBreak(Lnet/minecraft/world/level/block/state/BlockState;F)Z", at = @At("RETURN"))
    private boolean trackwright$keepMarkedBlocks(boolean original) {
        if (!original || breakingPos == null) {
            return original;
        }
        Level level = ((BlockEntity) (Object) this).getLevel();
        return level == null || !Markers.isProtected(level, breakingPos);
    }
}
