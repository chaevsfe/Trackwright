package dev.chaevsfe.trackwright.client.mixin;

import com.simibubi.create.content.trains.schedule.IScheduleInput;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleScreen;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import dev.chaevsfe.trackwright.schedule.ActorSteps;
import net.createmod.catnip.data.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(ScheduleScreen.class)
public abstract class ScheduleScreenMixin {
    @Shadow
    private SelectionScrollInput scrollInput;

    @Inject(method = "startEditing", at = @At("TAIL"))
    private void trackwright$hideActorStep(IScheduleInput field, Consumer<Boolean> onClose, boolean allowDeletion, CallbackInfo ci) {
        if (!(field instanceof ScheduleInstruction instruction) || instruction.getId().equals(ActorSteps.ID) || ActorSteps.offered()) {
            return;
        }
        List<Pair<ResourceLocation, Supplier<? extends ScheduleInstruction>>> types = Schedule.INSTRUCTION_TYPES;
        int hidden = -1;
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i).getFirst().equals(ActorSteps.ID)) {
                hidden = i;
            }
        }
        if (hidden < 0) {
            return;
        }
        int skipped = hidden;
        Consumer<Integer> original = ((ScrollInputAccessor) scrollInput).trackwright$onScroll();
        List<Component> options = new ArrayList<>(Schedule.getTypeOptions(types));
        options.remove(skipped);
        int state = scrollInput.getState();
        scrollInput.forOptions(options);
        scrollInput.calling(index -> {
            if (original != null) {
                original.accept(index >= skipped ? index + 1 : index);
            }
        });
        scrollInput.setState(state > skipped ? state - 1 : state);
    }
}
