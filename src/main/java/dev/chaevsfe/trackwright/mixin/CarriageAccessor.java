package dev.chaevsfe.trackwright.mixin;

import com.zurrtum.create.content.trains.entity.Carriage;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Carriage.class)
public interface CarriageAccessor {
    @Accessor("serialisedEntity")
    CompoundTag trackwright$serialisedEntity();
}
