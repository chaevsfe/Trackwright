package dev.chaevsfe.trackwright.client.controls;

import com.mojang.blaze3d.platform.InputConstants;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.Create;
import com.zurrtum.create.client.content.contraptions.ContraptionHandlerClient;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.zurrtum.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovingInteraction;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.infrastructure.packet.c2s.ContraptionInteractionPacket;
import dev.chaevsfe.trackwright.actors.TrainControlMessages;
import dev.chaevsfe.trackwright.actors.TrainControls;
import dev.chaevsfe.trackwright.client.guide.ActionBarHold;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.SettingMessages;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.network.TrainControlPayload;
import dev.chaevsfe.trackwright.schedule.ActorSwitch;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class TrainControlKeys {
    public static final int SLOTS = 5;
    public static final String KEY_1 = "trackwright.key.train_control_1";
    public static final String KEY_2 = "trackwright.key.train_control_2";
    public static final String KEY_3 = "trackwright.key.train_control_3";
    public static final String KEY_4 = "trackwright.key.train_control_4";
    public static final String KEY_5 = "trackwright.key.train_control_5";
    public static final String KEY_OPEN = "trackwright.key.train_controls";
    private static final String[] KEY_NAMES = {KEY_1, KEY_2, KEY_3, KEY_4, KEY_5};
    private static final int CHECK_TICKS = 20;

    private static final KeyMapping[] KEYS = new KeyMapping[SLOTS];
    private static final @Nullable ControlTarget[] TARGETS = new ControlTarget[SLOTS];
    private static @Nullable KeyMapping openKey;
    private static int sinceCheck;

    private TrainControlKeys() {
    }

    public static void register(KeyMapping.Category category) {
        for (int i = 0; i < SLOTS; i++) {
            KEYS[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY_NAMES[i], InputConstants.UNKNOWN.getValue(), category));
        }
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY_OPEN, InputConstants.UNKNOWN.getValue(), category));
    }

    public static void tick(Minecraft mc) {
        for (int i = 0; i < SLOTS; i++) {
            KeyMapping key = KEYS[i];
            if (key == null) {
                continue;
            }
            while (key.consumeClick()) {
                press(mc, i);
            }
        }
        KeyMapping open = openKey;
        if (open != null) {
            while (open.consumeClick()) {
                openTrainScreen(mc);
            }
        }
        if (++sinceCheck >= CHECK_TICKS) {
            sinceCheck = 0;
            dropGoneTrains(mc.level);
        }
    }

    public static void reset() {
        for (int i = 0; i < SLOTS; i++) {
            TARGETS[i] = null;
        }
        sinceCheck = 0;
    }

    public static @Nullable ControlTarget slot(int index) {
        return TARGETS[index];
    }

    public static int slotOf(ControlTarget target) {
        for (int i = 0; i < SLOTS; i++) {
            ControlTarget held = TARGETS[i];
            if (held != null && held.same(target)) {
                return i;
            }
        }
        return -1;
    }

    public static void assign(int index, ControlTarget target) {
        ControlTarget held = TARGETS[index];
        if (held != null && held.same(target)) {
            TARGETS[index] = null;
            return;
        }
        for (int i = 0; i < SLOTS; i++) {
            ControlTarget other = TARGETS[i];
            if (other != null && other.same(target)) {
                TARGETS[i] = null;
            }
        }
        TARGETS[index] = target;
    }

    public static Component keyName(int index) {
        KeyMapping key = KEYS[index];
        if (key == null || key.isUnbound()) {
            return Component.translatable("trackwright.train_controls.unbound");
        }
        return key.getTranslatedKeyMessage();
    }

    public static void press(Minecraft mc, int index) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return;
        }
        ControlTarget target = TARGETS[index];
        if (target == null) {
            tell(player, TrainControlMessages.refusal(TrainControlMessages.UNASSIGNED, index + 1));
            return;
        }
        if (!trainKnown(level, target.trainId())) {
            TARGETS[index] = null;
            tell(player, TrainControlMessages.refusal(TrainControlMessages.GONE, index + 1));
            return;
        }
        toggle(player, level, target);
    }

    public static void toggle(LocalPlayer player, ClientLevel level, ControlTarget target) {
        CarriageContraptionEntity entity = findCarriage(level, target.trainId(), target.carriageIndex());
        if (ClientPlayNetworking.canSend(TrainControlPayload.TYPE)) {
            TrackwrightSettings settings = RemoteSettings.get();
            if (settings == null || !settings.contraptionControlsHotkeys()) {
                tell(player, SettingMessages.component(SettingMessages.HOTKEYS_OFF).withStyle(ChatFormatting.RED));
                return;
            }
            ClientPlayNetworking.send(TrainControlPayload.of(target.trainId(), target.carriageIndex(), target.localPos(), target.actorId()));
            if (entity != null && TrainControls.isControl(entity.getContraption(), target.localPos(), target.actorId())) {
                entity.handlePlayerInteraction(player, target.localPos(), Direction.UP, InteractionHand.MAIN_HAND);
            }
            return;
        }
        if (entity == null) {
            tell(player, TrainControlMessages.refusal(TrainControlMessages.NOT_LOADED));
            return;
        }
        if (!TrainControls.isControl(entity.getContraption(), target.localPos(), target.actorId())) {
            tell(player, TrainControlMessages.refusal(TrainControlMessages.CHANGED));
            return;
        }
        if (!TrainControls.withinClick(player, entity)) {
            tell(player, TrainControlMessages.refusal(TrainControlMessages.TOO_FAR_CREATE));
            return;
        }
        if (entity.handlePlayerInteraction(player, target.localPos(), Direction.UP, InteractionHand.MAIN_HAND)) {
            player.connection.send(new ContraptionInteractionPacket(entity, InteractionHand.MAIN_HAND, target.localPos(), Direction.UP));
        }
    }

    public static boolean trainKnown(Level level, UUID trainId) {
        return Create.RAILWAYS.sided(level).trains.containsKey(trainId);
    }

    public static @Nullable Train train(Level level, UUID trainId) {
        return Create.RAILWAYS.sided(level).trains.get(trainId);
    }

    public static @Nullable CarriageContraptionEntity findCarriage(ClientLevel level, UUID trainId, int carriageIndex) {
        for (WeakReference<AbstractContraptionEntity> ref : ContraptionHandlerClient.loadedContraptions.get(level).values()) {
            if (ref.get() instanceof CarriageContraptionEntity carriage && carriage.isAlive()
                && trainId.equals(carriage.trainId) && carriage.carriageIndex == carriageIndex) {
                return carriage;
            }
        }
        return null;
    }

    public static @Nullable ControlTarget targetAt(CarriageContraptionEntity entity, BlockPos pos) {
        Contraption contraption = entity.getContraption();
        if (contraption == null || entity.trainId == null) {
            return null;
        }
        if (!(contraption.getInteractors().get(pos) instanceof ContraptionControlsMovingInteraction)) {
            return null;
        }
        StructureBlockInfo info = contraption.getBlocks().get(pos);
        if (info == null || !info.state().is(AllBlocks.CONTRAPTION_CONTROLS)) {
            return null;
        }
        MutablePair<StructureBlockInfo, @Nullable MovementContext> actor = contraption.getActorAt(pos);
        if (actor == null || actor.right == null) {
            return null;
        }
        ItemStack filter = ContraptionControlsMovement.getFilter(actor.right);
        ItemStack stack = filter == null ? ItemStack.EMPTY : filter.copy();
        return new ControlTarget(entity.trainId, entity.carriageIndex, pos.immutable(), ActorSwitch.idOf(stack), stack);
    }

    public static List<ControlTarget> controlsOn(CarriageContraptionEntity entity) {
        List<ControlTarget> found = new ArrayList<>();
        Contraption contraption = entity.getContraption();
        if (contraption == null) {
            return found;
        }
        for (MutablePair<StructureBlockInfo, MovementContext> pair : contraption.getActors()) {
            ControlTarget target = targetAt(entity, pair.left.pos());
            if (target != null) {
                found.add(target);
            }
        }
        found.sort(Comparator.comparingInt((ControlTarget t) -> t.localPos().getY())
            .thenComparingInt(t -> t.localPos().getZ())
            .thenComparingInt(t -> t.localPos().getX()));
        return found;
    }

    public static boolean isOff(CarriageContraptionEntity entity, ControlTarget target) {
        Contraption contraption = entity.getContraption();
        return contraption != null && (contraption.isActorTypeDisabled(target.filter()) || contraption.isActorTypeDisabled(ItemStack.EMPTY));
    }

    public static void openAssign(Minecraft mc, ControlTarget target) {
        mc.setScreenAndShow(new TrainControlsScreen(target.trainId(), target, false));
    }

    private static void openTrainScreen(Minecraft mc) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return;
        }
        UUID trainId = trainAround(player, level);
        if (trainId == null) {
            tell(player, TrainControlMessages.refusal(TrainControlMessages.NO_TRAIN));
            return;
        }
        mc.setScreenAndShow(new TrainControlsScreen(trainId, null, true));
    }

    private static @Nullable UUID trainAround(LocalPlayer player, ClientLevel level) {
        for (Entity vehicle = player.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
            if (vehicle instanceof CarriageContraptionEntity carriage && carriage.trainId != null) {
                return carriage.trainId;
            }
        }
        for (WeakReference<AbstractContraptionEntity> ref : ContraptionHandlerClient.loadedContraptions.get(level).values()) {
            if (ref.get() instanceof CarriageContraptionEntity carriage && carriage.isAlive() && carriage.trainId != null
                && carriage.getBoundingBox().contains(player.position())) {
                return carriage.trainId;
            }
        }
        return null;
    }

    private static void dropGoneTrains(@Nullable ClientLevel level) {
        if (level == null) {
            return;
        }
        for (int i = 0; i < SLOTS; i++) {
            ControlTarget target = TARGETS[i];
            if (target != null && !trainKnown(level, target.trainId())) {
                TARGETS[i] = null;
            }
        }
    }

    private static void tell(LocalPlayer player, Component message) {
        player.sendOverlayMessage(message);
        ActionBarHold.start();
    }
}
