package dev.chaevsfe.trackwright.schedule;

import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import dev.chaevsfe.trackwright.config.ServerConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class ActorStepInstruction extends ScheduleInstruction {
    public static final String ACTOR = "Actor";
    public static final String MODE = "Mode";
    public static final int TURN_ON = 0;
    public static final int TURN_OFF = 1;

    public ActorStepInstruction(Identifier id) {
        super(id);
        data.putString(ACTOR, "");
        data.putInt(MODE, TURN_ON);
    }

    @Override
    public boolean supportsConditions() {
        return false;
    }

    public String actorId() {
        return data.getStringOr(ACTOR, "");
    }

    public void setActor(ItemStack stack) {
        data.putString(ACTOR, ActorSwitch.idOf(stack));
    }

    public @Nullable ItemStack actor() {
        return ActorSwitch.stackOf(actorId());
    }

    public boolean turnsOn() {
        return intData(MODE) != TURN_OFF;
    }

    @Override
    public @Nullable DiscoveredPath start(ScheduleRuntime runtime, Level level) {
        ItemStack actor = actor();
        if (actor != null && !level.isClientSide() && ServerConfig.get().contraptionControlsSchedule()) {
            ActorSwitch.applyToTrain(runtime.train, actor, turnsOn());
        }
        runtime.state = ScheduleRuntime.State.PRE_TRANSIT;
        runtime.currentEntry++;
        return null;
    }
}
