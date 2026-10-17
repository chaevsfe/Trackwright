package dev.chaevsfe.trackwright.schedule;

import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.trains.entity.Carriage;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.infrastructure.packet.s2c.ContraptionDisableActorPacket;
import dev.chaevsfe.trackwright.mixin.CarriageAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.storage.ValueInput;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class ActorSwitch {
    public static final String PENDING = "TrackwrightActorSteps";
    private static final String CONTRAPTION = "Contraption";
    private static final String ACTOR = "Actor";
    private static final String ON = "On";
    private static final int MAX_PENDING = 32;

    @FunctionalInterface
    public interface Sender {
        void send(ItemStack filter, boolean enable);
    }

    private static final Sender NOBODY = (filter, enable) -> {
    };

    private ActorSwitch() {
    }

    public static String idOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static @Nullable ItemStack stackOf(String id) {
        if (id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Identifier key = Identifier.tryParse(id);
        if (key == null) {
            return null;
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(key);
        if (item.isEmpty() || item.get() == Items.AIR) {
            return null;
        }
        return new ItemStack(item.get());
    }

    public static void applyToTrain(Train train, ItemStack target, boolean on) {
        for (Carriage carriage : train.carriages) {
            boolean[] live = new boolean[1];
            carriage.forEachPresentEntity(entity -> {
                Contraption contraption = entity.getContraption();
                if (contraption == null) {
                    return;
                }
                live[0] = true;
                apply(contraption, target, on, (filter, enable) -> send(entity, filter, enable));
            });
            CompoundTag saved = ((CarriageAccessor) carriage).trackwright$serialisedEntity();
            if (live[0]) {
                saved.getCompound(CONTRAPTION).ifPresent(contraption -> contraption.remove(PENDING));
            } else {
                queue(saved, target, on);
            }
        }
    }

    public static void apply(Contraption contraption, ItemStack target, boolean on, Sender sender) {
        List<ItemStack> disabled = contraption.getDisabledActors();
        if (target.isEmpty()) {
            disabled.clear();
            if (!on) {
                disabled.add(ItemStack.EMPTY);
            }
            contraption.setActorsActive(ItemStack.EMPTY, on);
            sender.send(ItemStack.EMPTY, on);
            return;
        }
        boolean allOff = disabled.stream().anyMatch(ItemStack::isEmpty);
        if (!on) {
            if (allOff || contraption.isActorTypeDisabled(target)) {
                return;
            }
            disabled.add(target.copy());
            contraption.setActorsActive(target, false);
            sender.send(target, false);
            return;
        }
        if (allOff) {
            disabled.removeIf(ItemStack::isEmpty);
            for (MutablePair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
                MovementContext context = pair.getRight();
                if (context == null) {
                    continue;
                }
                MovementBehaviour behaviour = MovementBehaviour.REGISTRY.get(pair.getLeft().state());
                if (behaviour == null) {
                    continue;
                }
                ItemStack type = behaviour.canBeDisabledVia(context);
                if (type == null || ContraptionControlsMovement.isSameFilter(type, target) || contraption.isActorTypeDisabled(type)) {
                    continue;
                }
                disabled.add(type);
                sender.send(type, false);
            }
        }
        boolean wasOff = disabled.removeIf(stack -> ContraptionControlsMovement.isSameFilter(stack, target));
        if (!allOff && !wasOff) {
            return;
        }
        contraption.setActorsActive(target, true);
        sender.send(target, true);
    }

    public static void applyPending(Contraption contraption, ValueInput view) {
        for (ValueInput step : view.childrenListOrEmpty(PENDING)) {
            ItemStack target = stackOf(step.getStringOr(ACTOR, ""));
            if (target != null) {
                apply(contraption, target, step.getBooleanOr(ON, true), NOBODY);
            }
        }
    }

    private static void queue(CompoundTag entity, ItemStack target, boolean on) {
        Optional<CompoundTag> found = entity.getCompound(CONTRAPTION);
        if (found.isEmpty()) {
            return;
        }
        CompoundTag contraption = found.get();
        String id = idOf(target);
        ListTag kept = new ListTag();
        if (!id.isEmpty()) {
            for (Tag tag : contraption.getListOrEmpty(PENDING)) {
                if (tag instanceof CompoundTag step && !step.getStringOr(ACTOR, "").equals(id)) {
                    kept.add(step);
                }
            }
        }
        CompoundTag step = new CompoundTag();
        step.putString(ACTOR, id);
        step.putBoolean(ON, on);
        kept.add(step);
        while (kept.size() > MAX_PENDING) {
            kept.remove(0);
        }
        contraption.put(PENDING, kept);
    }

    private static void send(CarriageContraptionEntity entity, ItemStack filter, boolean enable) {
        if (entity.level() instanceof ServerLevel level) {
            level.getChunkSource().sendToTrackingPlayers(entity, new ContraptionDisableActorPacket(entity.getId(), filter.copy(), enable));
        }
    }
}
