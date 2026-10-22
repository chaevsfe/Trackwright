package dev.chaevsfe.trackwright.client.schedule;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.schedule.IScheduleInput;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import dev.chaevsfe.trackwright.schedule.ActorStepInstruction;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ActorStepRender implements IScheduleInput<ActorStepInstruction> {
    @Override
    public Pair<ItemStack, Component> getSummary(ActorStepInstruction input) {
        ItemStack actor = input.actor();
        return Pair.of(actor == null || actor.isEmpty() ? icon() : actor, state(input));
    }

    @Override
    public int slotsTargeted() {
        return 1;
    }

    @Override
    public List<Component> getTitleAs(ActorStepInstruction input, String type) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("trackwright.schedule.instruction.contraption_controls").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("trackwright.schedule.contraption_controls.target", target(input), state(input)).withStyle(ChatFormatting.DARK_AQUA));
        if (!ActorStepsClient.offered()) {
            lines.add(Component.translatable("trackwright.schedule.contraption_controls.server_off").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    public ItemStack getSecondLineIcon() {
        return icon();
    }

    @Override
    public void setItem(ActorStepInstruction input, int slot, ItemStack stack) {
        input.setActor(stack);
    }

    @Override
    public ItemStack getItem(ActorStepInstruction input, int slot) {
        ItemStack actor = input.actor();
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
    public void initConfigurationWidgets(ActorStepInstruction input, ModularGuiLineBuilder builder) {
        builder.addSelectionScrollInput(
            0, 60, (scroll, label) -> scroll.forOptions(ImmutableList.of(
                Component.translatable("trackwright.schedule.contraption_controls.turn_on"),
                Component.translatable("trackwright.schedule.contraption_controls.turn_off")
            )).titled(Component.translatable("trackwright.schedule.contraption_controls.mode")), ActorStepInstruction.MODE
        );
    }

    private static MutableComponent target(ActorStepInstruction input) {
        ItemStack actor = input.actor();
        if (actor == null) {
            return Component.literal(input.actorId());
        }
        if (actor.isEmpty()) {
            return Component.translatable("trackwright.schedule.contraption_controls.all");
        }
        return actor.getHoverName().copy();
    }

    private static MutableComponent state(ActorStepInstruction input) {
        return input.turnsOn()
            ? Component.translatable("trackwright.schedule.contraption_controls.on")
            : Component.translatable("trackwright.schedule.contraption_controls.off");
    }

    private static ItemStack icon() {
        return AllItems.CONTRAPTION_CONTROLS.getDefaultInstance();
    }
}
