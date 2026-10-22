package dev.chaevsfe.trackwright.schedule;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import dev.chaevsfe.trackwright.TagReads;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class ActorSteps {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Trackwright.MOD_ID, "contraption_controls");
    public static final String NEEDS_TRACKWRIGHT = "trackwright.schedule.needs_trackwright";
    private static final String NEEDS_TRACKWRIGHT_TEXT = "This schedule has Trackwright steps, so it can only be changed with Trackwright installed";

    private ActorSteps() {
    }

    public static synchronized void register() {
        if (Schedule.INSTRUCTION_TYPES.stream().noneMatch(type -> type.getFirst().equals(ID))) {
            Schedule.INSTRUCTION_TYPES.add(Pair.<ResourceLocation, Supplier<? extends ScheduleInstruction>>of(ID, ActorStepInstruction::new));
        }
        if (!(create(ID) instanceof ActorStepInstruction)) {
            throw new IllegalStateException("Trackwright could not add its schedule step " + ID + " to Create's schedule list");
        }
    }

    private static @Nullable ScheduleInstruction create(ResourceLocation id) {
        for (Pair<ResourceLocation, Supplier<? extends ScheduleInstruction>> type : Schedule.INSTRUCTION_TYPES) {
            if (type.getFirst().equals(id)) {
                return type.getSecond().get();
            }
        }
        return null;
    }

    public static boolean offered() {
        TrackwrightSettings settings = RemoteSettings.get();
        return settings != null && settings.contraptionControlsSchedule();
    }

    public static boolean refuseEdit(ServerPlayer player) {
        if (TrackwrightNetwork.hasTrackwright(player)) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(AllItems.SCHEDULE)) {
            return false;
        }
        CompoundTag schedule = held.get(AllDataComponents.TRAIN_SCHEDULE);
        if (schedule == null || !contains(schedule)) {
            return false;
        }
        player.sendSystemMessage(Component.translatableWithFallback(NEEDS_TRACKWRIGHT, NEEDS_TRACKWRIGHT_TEXT).withStyle(ChatFormatting.RED));
        return true;
    }

    public static boolean contains(CompoundTag schedule) {
        String id = ID.toString();
        for (Tag entry : schedule.getList("Entries", Tag.TAG_COMPOUND)) {
            if (entry instanceof CompoundTag compound && TagReads.stringOr(compound.getCompound("Instruction"), "Id", "").equals(id)) {
                return true;
            }
        }
        return false;
    }
}
