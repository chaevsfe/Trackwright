package dev.chaevsfe.trackwright.client.mixin;

import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScrollValueRenderer.class)
public interface ScrollValueRendererAccessor {
    @Invoker("addBox")
    static void trackwright$addBox(BlockPos pos, Direction face, ScrollValueBehaviour<?, ?> behaviour, boolean highlight) {
        throw new AssertionError();
    }
}
