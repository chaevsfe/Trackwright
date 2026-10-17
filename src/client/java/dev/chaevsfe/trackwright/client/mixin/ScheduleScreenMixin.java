package dev.chaevsfe.trackwright.client.mixin;

import com.zurrtum.create.AllSchedules;
import com.zurrtum.create.catnip.data.Pair;
import com.zurrtum.create.client.content.trains.schedule.IScheduleInput;
import com.zurrtum.create.client.content.trains.schedule.ScheduleScreen;
import com.zurrtum.create.client.foundation.gui.widget.SelectionScrollInput;
import com.zurrtum.create.content.trains.schedule.ScheduleDataEntry;
import com.zurrtum.create.content.trains.schedule.destination.ScheduleInstruction;
import dev.chaevsfe.trackwright.client.schedule.ActorStepsClient;
import dev.chaevsfe.trackwright.schedule.ActorSteps;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(ScheduleScreen.class)
public abstract class ScheduleScreenMixin {
    @Shadow
    private SelectionScrollInput scrollInput;

    @Inject(method = "startEditing", at = @At("TAIL"))
    private void trackwright$hideActorStep(IScheduleInput<?> field, ScheduleDataEntry input, Consumer<Boolean> onClose, boolean allowDeletion, CallbackInfo ci) {
        if (!(input instanceof ScheduleInstruction instruction) || instruction.getId().equals(ActorSteps.ID) || ActorStepsClient.offered()) {
            return;
        }
        List<Pair<Identifier, Function<Identifier, ? extends ScheduleInstruction>>> types = AllSchedules.INSTRUCTION_TYPES;
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
        List<MutableComponent> options = new ArrayList<>(ScheduleScreen.getTypeOptions(types));
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
