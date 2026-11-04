package dev.chaevsfe.trackwright.actors;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovingInteraction;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.config.SettingMessages;
import dev.chaevsfe.trackwright.network.TrainControlPayload;
import dev.chaevsfe.trackwright.schedule.ActorSwitch;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TrainControls {
    public enum Result {
        OK,
        OFF,
        BUSY,
        BAD,
        GONE,
        NOT_LOADED,
        TOO_FAR,
        CHANGED
    }

    private static final int PRESS_GAP_TICKS = 2;
    private static final double CLICK_REACH_EXTRA = 10;
    private static final double NO_SIGHT_PENALTY = 3;
    private static final Map<UUID, Integer> LAST_PRESS = new ConcurrentHashMap<>();
    private static volatile boolean warnedNewer;

    private TrainControls() {
    }

    public static Result receive(ServerPlayer player, TrainControlPayload payload) {
        if (payload.version() > TrainControlPayload.VERSION && !warnedNewer) {
            warnedNewer = true;
            Trackwright.LOGGER.info("A client sends train control format {} and this server reads format {}; only the fields both know are used",
                payload.version(), TrainControlPayload.VERSION);
        }
        Result result = press(player, payload);
        Component message = refusal(result);
        if (message != null) {
            player.displayClientMessage(message, true);
        }
        return result;
    }

    public static void forget(UUID player) {
        LAST_PRESS.remove(player);
    }

    public static void clearAll() {
        LAST_PRESS.clear();
    }

    private static Result press(ServerPlayer player, TrainControlPayload payload) {
        if (!ServerConfig.get().contraptionControlsHotkeys()) {
            return Result.OFF;
        }
        if (player.isSpectator()) {
            return Result.BAD;
        }
        if (!acceptPress(player)) {
            return Result.BUSY;
        }
        TrainControlPayload.Press press = payload.press();
        if (press == null) {
            return Result.BAD;
        }
        Train train = Create.RAILWAYS.sided(player.level()).trains.get(press.train());
        if (train == null || train.invalid) {
            return Result.GONE;
        }
        if (press.carriage() < 0 || press.carriage() >= train.carriages.size()) {
            return Result.GONE;
        }
        Carriage carriage = train.carriages.get(press.carriage());
        Carriage.DimensionalCarriageEntity dimensional = carriage.getDimensionalIfPresent(player.level().dimension());
        CarriageContraptionEntity entity = dimensional == null ? null : dimensional.entity.get();
        if (entity == null || !entity.isAlive()) {
            return Result.NOT_LOADED;
        }
        if (!rides(player, train.id) && !withinClick(player, entity)) {
            return Result.TOO_FAR;
        }
        if (!isControl(entity.getContraption(), press.pos(), press.actor())) {
            return Result.CHANGED;
        }
        entity.handlePlayerInteraction(player, press.pos(), Direction.UP, InteractionHand.MAIN_HAND);
        return Result.OK;
    }

    public static boolean isControl(@Nullable Contraption contraption, BlockPos pos, String actor) {
        if (contraption == null) {
            return false;
        }
        StructureBlockInfo info = contraption.getBlocks().get(pos);
        if (info == null || !info.state().is(AllBlocks.CONTRAPTION_CONTROLS.get())) {
            return false;
        }
        MovingInteractionBehaviour interactor = contraption.getInteractors().get(pos);
        if (!(interactor instanceof ContraptionControlsMovingInteraction)) {
            return false;
        }
        String present = actorId(contraption, pos);
        return present != null && present.equals(actor);
    }

    public static @Nullable String actorId(Contraption contraption, BlockPos pos) {
        MutablePair<StructureBlockInfo, MovementContext> actor = contraption.getActorAt(pos);
        if (actor == null || actor.right == null) {
            return null;
        }
        ItemStack filter = ContraptionControlsMovement.getFilter(actor.right);
        return ActorSwitch.idOf(filter == null ? ItemStack.EMPTY : filter);
    }

    public static boolean rides(Entity player, UUID trainId) {
        for (Entity vehicle = player.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
            if (vehicle instanceof CarriageContraptionEntity carriage && trainId.equals(carriage.trainId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean withinClick(Player player, Entity entity) {
        AABB box = entity.getBoundingBox();
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + CLICK_REACH_EXTRA + Math.max(box.getXsize(), box.getYsize());
        if (!player.hasLineOfSight(entity)) {
            reach -= NO_SIGHT_PENALTY;
        }
        return player.distanceToSqr(entity) <= reach * reach;
    }

    private static boolean acceptPress(ServerPlayer player) {
        int now = player.level().getServer().getTickCount();
        Integer last = LAST_PRESS.get(player.getUUID());
        if (last != null && now - last < PRESS_GAP_TICKS && now >= last) {
            return false;
        }
        LAST_PRESS.put(player.getUUID(), now);
        return true;
    }

    private static @Nullable Component refusal(Result result) {
        return switch (result) {
            case OFF -> SettingMessages.component(SettingMessages.HOTKEYS_OFF).withStyle(ChatFormatting.RED);
            case GONE -> TrainControlMessages.refusal(TrainControlMessages.TRAIN_GONE);
            case NOT_LOADED -> TrainControlMessages.refusal(TrainControlMessages.NOT_LOADED);
            case TOO_FAR -> TrainControlMessages.refusal(TrainControlMessages.TOO_FAR);
            case CHANGED -> TrainControlMessages.refusal(TrainControlMessages.CHANGED);
            case OK, BUSY, BAD -> null;
        };
    }
}
