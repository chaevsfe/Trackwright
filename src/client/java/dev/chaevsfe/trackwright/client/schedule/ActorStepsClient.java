package dev.chaevsfe.trackwright.client.schedule;

import com.simibubi.create.AllScheduleRenders;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.schedule.ActorSteps;

public final class ActorStepsClient {
    private ActorStepsClient() {
    }

    public static void register() {
        AllScheduleRenders.ALL.put(ActorSteps.ID, new ActorStepRender());
    }

    public static boolean offered() {
        TrackwrightSettings settings = RemoteSettings.get();
        return settings != null && settings.contraptionControlsSchedule();
    }
}
