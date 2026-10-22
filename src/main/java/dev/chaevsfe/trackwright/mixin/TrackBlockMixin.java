package dev.chaevsfe.trackwright.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.track.TrackBlock;
import dev.chaevsfe.trackwright.track.PlacementRules;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TrackBlock.class)
public abstract class TrackBlockMixin {
    @ModifyReturnValue(method = "getStateForPlacement", at = @At("RETURN"))
    private BlockState trackwright$keepClearOfStraights(BlockState state, @Local(argsOnly = true) BlockPlaceContext context) {
        return PlacementRules.guardSinglePlacement(state, context);
    }
}
