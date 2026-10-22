package dev.chaevsfe.trackwright.client.mixin;

import com.simibubi.create.foundation.gui.widget.ScrollInput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Consumer;

@Mixin(ScrollInput.class)
public interface ScrollInputAccessor {
    @Accessor("onScroll")
    @Nullable Consumer<Integer> trackwright$onScroll();
}
