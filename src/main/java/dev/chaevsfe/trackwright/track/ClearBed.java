package dev.chaevsfe.trackwright.track;

import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.BoreMode;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.config.SettingMessages;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import dev.chaevsfe.trackwright.network.ClearBedPayload;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClearBed {
    private static final int OFFER_TICKS = 200;
    private static final int DUST_PERIOD = 20;
    private static final int DUST_CAP = 512;
    private static final double AIM_SLACK = 1;
    private static final int HOLD_REPEAT = 6;
    private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(0xF0 / 255f, 0xA0 / 255f, 0x30 / 255f), 1.0f);
    private static final Map<UUID, Offer> OFFERS = new HashMap<>();
    private static final Map<UUID, Handled> HANDLED = new HashMap<>();
    private static boolean warnedNewer;

    private record Offer(ResourceKey<Level> dimension, BlockPos track, BlockPos target, long hash, long made, long expires, List<BlockPos> dust) {
        boolean covers(ResourceKey<Level> level, BlockPos from, BlockPos to) {
            return dimension.equals(level) && (track.equals(from) && target.equals(to) || track.equals(to) && target.equals(from));
        }
    }

    private record Handled(long tick, BlockPos pos) {
    }

    private ClearBed() {
    }

    public static void register(IEventBus game) {
        game.addListener(PlayerInteractEvent.RightClickBlock.class, ClearBed::onRightClickBlock);
        game.addListener(ServerTickEvent.Post.class, event -> tick(event.getServer()));
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> forget(event.getEntity().getUUID()));
        game.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> forget(event.getEntity().getUUID()));
        game.addListener(ServerStoppedEvent.class, event -> clearAll());
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = onUseBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    public static void forget(UUID player) {
        OFFERS.remove(player);
        HANDLED.remove(player);
    }

    public static void clearAll() {
        OFFERS.clear();
        HANDLED.clear();
    }

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)
            || player.isSpectator() || player instanceof DeployerFakePlayer) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        long now = serverLevel.getServer().getTickCount();
        if (hand == InteractionHand.OFF_HAND) {
            Handled last = HANDLED.get(player.getUUID());
            return last != null && now - last.tick() <= 1 && last.pos().equals(pos) ? InteractionResult.FAIL : InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown() || !BlockMiner.isTool(player.getMainHandItem())
            || !(level.getBlockState(pos).getBlock() instanceof ITrackBlock)
            || !(level.getBlockEntity(pos) instanceof TrackBlockEntity track) || track.getConnections().isEmpty()) {
            return InteractionResult.PASS;
        }
        TrackwrightSettings settings = ServerConfig.get();
        if (settings.clearTrackBed() == BoreMode.OFF) {
            return InteractionResult.PASS;
        }
        Handled previous = HANDLED.put(player.getUUID(), new Handled(now, pos.immutable()));
        if (previous != null && previous.pos().equals(pos) && now - previous.tick() <= HOLD_REPEAT) {
            return InteractionResult.SUCCESS;
        }
        if (!settings.clearTrackBed().allows(player)) {
            refuse(serverPlayer, SettingMessages.component(SettingMessages.CLEAR_CREATIVE_ONLY));
            return InteractionResult.FAIL;
        }
        BezierConnection connection = ClearBedPlan.pick(track, player.getLookAngle());
        if (connection == null) {
            refuse(serverPlayer, ClearMessages.component(ClearMessages.NO_CONNECTION));
            return InteractionResult.FAIL;
        }
        click(serverPlayer, serverLevel, track, connection, null, -1, settings);
        return InteractionResult.SUCCESS;
    }

    public static void request(ServerPlayer player, ClearBedPayload payload) {
        if (payload.version() > ClearBedPayload.VERSION && !warnedNewer) {
            warnedNewer = true;
            Trackwright.LOGGER.info("A client sent clear_bed format {} and this server reads format {}; only the fields both know are used",
                payload.version(), ClearBedPayload.VERSION);
        }
        TrackwrightSettings settings = ServerConfig.get();
        if (!settings.clearTrackBed().allows(player)) {
            refuse(player, SettingMessages.component(settings.clearTrackBed() == BoreMode.OFF ? SettingMessages.CLEAR_OFF : SettingMessages.CLEAR_CREATIVE_ONLY));
            return;
        }
        if (player instanceof DeployerFakePlayer || !player.isAlive() || player.isSpectator() || !BlockMiner.isTool(player.getMainHandItem())) {
            return;
        }
        BlockPos pos = payload.track();
        BlockPos target = payload.target();
        Vec3 aim = payload.aim();
        if (pos == null || target == null || aim == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        int reach = ConnectionLength.limit(AllConfigs.server().trains.maxTrackPlacementLength.get(), settings) + ConnectionLength.CURVE_REACH_MARGIN;
        if (!level.isLoaded(pos) || player.position().distanceToSqr(Vec3.atCenterOf(pos)) > (double) reach * reach) {
            refuse(player, ClearMessages.component(ClearMessages.TOO_FAR));
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof TrackBlockEntity track)) {
            refuse(player, ClearMessages.component(ClearMessages.NO_CONNECTION));
            return;
        }
        BezierConnection connection = track.getConnections().get(target);
        if (connection == null) {
            refuse(player, ClearMessages.component(ClearMessages.NO_CONNECTION));
            return;
        }
        boolean onConnection = connection.getBounds().inflate(AIM_SLACK).contains(aim) || new AABB(pos).inflate(AIM_SLACK).contains(aim);
        if (!onConnection || !player.canInteractWithBlock(BlockPos.containing(aim), 1.0)) {
            refuse(player, ClearMessages.component(ClearMessages.TOO_FAR));
            return;
        }
        BoreProfiles.receive(player, payload.request());
        click(player, level, track, connection, payload.plan(), payload.count(), settings);
    }

    private static void click(ServerPlayer player, ServerLevel level, TrackBlockEntity track, BezierConnection connection, @Nullable Long shown,
                              int shownCount, TrackwrightSettings settings) {
        UUID id = player.getUUID();
        long now = level.getServer().getTickCount();
        BoreProfile profile = BoreProfiles.resolve(level, player, connection.getMaterial(), settings);
        ClearBedPlan plan = ClearBedPlan.of(level, track.getBlockPos(), connection, player.getMainHandItem(), player.isCreative(), profile, settings);
        BorePlan bore = plan.bore();
        Offer offer = OFFERS.get(id);
        if (offer != null && now >= offer.expires()) {
            OFFERS.remove(id);
            offer = null;
        }
        boolean sameConnection = offer != null && offer.covers(level.dimension(), plan.track(), plan.target());
        if (bore.problem() != null) {
            OFFERS.remove(id);
            refuse(player, ClearMessages.refusal(bore.problem()));
            return;
        }
        if (bore.cells().isEmpty()) {
            OFFERS.remove(id);
            refuse(player, ClearMessages.component(ClearMessages.NOTHING));
            return;
        }
        BlockMiner.Problem permission = BlockMiner.checkPermission(level, player, bore.cells(), InteractionHand.MAIN_HAND);
        if (permission != null) {
            OFFERS.remove(id);
            refuse(player, ClearMessages.refusal(permission));
            return;
        }
        boolean confirmed = shown != null && shown == plan.hash() && shownCount == plan.count()
            || sameConnection && offer.hash() == plan.hash();
        if (confirmed) {
            OFFERS.remove(id);
            clear(player, level, track, plan);
            return;
        }
        boolean changed = shown != null || sameConnection;
        List<BlockPos> cells = bore.cells();
        List<BlockPos> dust = List.copyOf(cells.size() > DUST_CAP ? cells.subList(0, DUST_CAP) : cells);
        OFFERS.put(id, new Offer(level.dimension(), plan.track(), plan.target(), plan.hash(), now, now + OFFER_TICKS, dust));
        MutableComponent message = changed
            ? ClearMessages.counted(ClearMessages.CHANGED, ClearMessages.CHANGED_ONE, plan.count())
            : ClearMessages.counted(ClearMessages.CONFIRM, ClearMessages.CONFIRM_ONE, plan.count());
        tell(player, message.withStyle(ChatFormatting.GOLD));
    }

    private static void clear(ServerPlayer player, ServerLevel level, TrackBlockEntity track, ClearBedPlan plan) {
        List<BlockPos> cells = plan.bore().cells();
        BlockMiner.Outcome outcome = BlockMiner.mine(level, player, cells, InteractionHand.MAIN_HAND);
        BlockPos pos = plan.track();
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof TrackBlockEntity live) {
            BezierConnection connection = live.getConnections().get(plan.target());
            if (connection != null) {
                live.manageFakeTracksAlong(connection, false);
            }
        }
        if (outcome.stoppedAt() != null) {
            refuse(player, ClearMessages.counted(ClearMessages.STOPPED, ClearMessages.STOPPED_ONE, outcome.mined()));
        } else {
            tell(player, ClearMessages.counted(ClearMessages.DONE, ClearMessages.DONE_ONE, outcome.mined()).withStyle(ChatFormatting.GREEN));
        }
    }

    private static void tick(MinecraftServer server) {
        if (OFFERS.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        Iterator<Map.Entry<UUID, Offer>> entries = OFFERS.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Offer> entry = entries.next();
            Offer offer = entry.getValue();
            if (now >= offer.expires()) {
                entries.remove();
                continue;
            }
            if ((now - offer.made()) % DUST_PERIOD != 0) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.level().dimension().equals(offer.dimension())) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            for (BlockPos cell : offer.dust()) {
                level.sendParticles(player, DUST, true, cell.getX() + 0.5, cell.getY() + 0.5, cell.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    private static void refuse(ServerPlayer player, MutableComponent message) {
        tell(player, message.withStyle(ChatFormatting.RED));
    }

    private static void tell(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
        if (!TrackwrightNetwork.hasTrackwright(player)) {
            player.sendSystemMessage(message);
        }
    }
}
