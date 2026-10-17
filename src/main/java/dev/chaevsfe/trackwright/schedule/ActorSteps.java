package dev.chaevsfe.trackwright.schedule;

import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.AllSchedules;
import com.zurrtum.create.catnip.data.Pair;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ActorSteps {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "contraption_controls");
    public static final String NEEDS_TRACKWRIGHT = "trackwright.schedule.needs_trackwright";
    private static final String NEEDS_TRACKWRIGHT_TEXT = "This schedule has Trackwright steps, so it can only be changed with Trackwright installed";

    private ActorSteps() {
    }

    public static synchronized void register() {
        if (AllSchedules.INSTRUCTION_TYPES.stream().noneMatch(type -> type.getFirst().equals(ID))) {
            AllSchedules.INSTRUCTION_TYPES.add(Pair.of(ID, ActorStepInstruction::new));
        }
        if (!(AllSchedules.createScheduleInstruction(ID) instanceof ActorStepInstruction)) {
            throw new IllegalStateException("Trackwright could not add its schedule step " + ID + " to Create's schedule list");
        }
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
        for (Tag entry : schedule.getListOrEmpty("Entries")) {
            if (entry instanceof CompoundTag compound && compound.getCompoundOrEmpty("Instruction").getStringOr("Id", "").equals(id)) {
                return true;
            }
        }
        return false;
    }
}
