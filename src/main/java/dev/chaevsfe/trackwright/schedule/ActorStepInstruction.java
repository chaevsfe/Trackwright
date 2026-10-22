package dev.chaevsfe.trackwright.schedule;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import dev.chaevsfe.trackwright.TagReads;
import dev.chaevsfe.trackwright.config.ServerConfig;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ActorStepInstruction extends ScheduleInstruction {
    public static final String ACTOR = "Actor";
    public static final String MODE = "Mode";
    public static final int TURN_ON = 0;
    public static final int TURN_OFF = 1;

    public ActorStepInstruction() {
        data.putString(ACTOR, "");
        data.putInt(MODE, TURN_ON);
    }

    @Override
    public ResourceLocation getId() {
        return ActorSteps.ID;
    }

    @Override
    public boolean supportsConditions() {
        return false;
    }

    public String actorId() {
        return TagReads.stringOr(data, ACTOR, "");
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

    @Override
    public Pair<ItemStack, Component> getSummary() {
        ItemStack actor = actor();
        return Pair.of(actor == null || actor.isEmpty() ? icon() : actor, state());
    }

    @Override
    public int slotsTargeted() {
        return 1;
    }

    @Override
    public List<Component> getTitleAs(String type) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("trackwright.schedule.instruction.contraption_controls").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("trackwright.schedule.contraption_controls.target", target(), state()).withStyle(ChatFormatting.DARK_AQUA));
        if (!ActorSteps.offered()) {
            lines.add(Component.translatable("trackwright.schedule.contraption_controls.server_off").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    public ItemStack getSecondLineIcon() {
        return icon();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setActor(stack);
    }

    @Override
    public ItemStack getItem(int slot) {
        ItemStack actor = actor();
        return actor == null ? ItemStack.EMPTY : actor;
    }

    @Override
    public List<Component> getSecondLineTooltip(int slot) {
        return ImmutableList.of(
            Component.translatable("trackwright.schedule.contraption_controls.slot"),
            Component.translatable("trackwright.schedule.contraption_controls.slot_1").withStyle(ChatFormatting.GRAY),
            Component.translatable("trackwright.schedule.contraption_controls.slot_2").withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
        builder.addSelectionScrollInput(
            0, 60, (scroll, label) -> scroll.forOptions(ImmutableList.of(
                Component.translatable("trackwright.schedule.contraption_controls.turn_on"),
                Component.translatable("trackwright.schedule.contraption_controls.turn_off")
            )).titled(Component.translatable("trackwright.schedule.contraption_controls.mode")), MODE
        );
    }

    private MutableComponent target() {
        ItemStack actor = actor();
        if (actor == null) {
            return Component.literal(actorId());
        }
        if (actor.isEmpty()) {
            return Component.translatable("trackwright.schedule.contraption_controls.all");
        }
        return actor.getHoverName().copy();
    }

    private MutableComponent state() {
        return turnsOn()
            ? Component.translatable("trackwright.schedule.contraption_controls.on")
            : Component.translatable("trackwright.schedule.contraption_controls.off");
    }

    private static ItemStack icon() {
        return AllBlocks.CONTRAPTION_CONTROLS.asStack();
    }
}
