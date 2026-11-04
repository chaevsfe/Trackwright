package dev.chaevsfe.trackwright.markers;

import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.MarkerMode;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.config.SettingMessages;
import dev.chaevsfe.trackwright.config.Sides;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public final class Markers {
    public static final TagKey<Item> TOOLS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Trackwright.MOD_ID, "marker_tools"));
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Trackwright.MOD_ID);
    public static final Supplier<AttachmentType<MarkerSet>> ATTACHMENT = ATTACHMENT_TYPES.register(
        "markers",
        () -> AttachmentType.builder(() -> MarkerSet.EMPTY).serialize(MarkerSet.CODEC, set -> !set.isEmpty()).build()
    );
    public static final int BOX_CAP = 32768;

    private static final int HOLD_REPEAT = 6;
    private static final Map<UUID, Handled> HANDLED = new ConcurrentHashMap<>();
    private static volatile @Nullable BiPredicate<Level, BlockPos> clientSource;
    private static volatile @Nullable BiPredicate<Level, BlockPos> clientCoverage;

    private record Handled(long tick, BlockPos pos) {
    }

    private Markers() {
    }

    public static void register(IEventBus modBus, IEventBus game) {
        ATTACHMENT_TYPES.register(modBus);
        game.addListener(EventPriority.LOWEST, false, PlayerInteractEvent.RightClickBlock.class, Markers::onRightClickBlock);
        game.addListener(RegisterCommandsEvent.class, event -> MarkerCommands.register(event.getDispatcher()));
        game.addListener(ServerTickEvent.Post.class, event -> MarkerSync.tick(event.getServer()));
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            MarkerSync.forget(event.getEntity().getUUID());
            HANDLED.remove(event.getEntity().getUUID());
        });
        game.addListener(ServerStoppedEvent.class, event -> {
            MarkerSync.clearAll();
            HANDLED.clear();
        });
    }

    public static void setClientSource(BiPredicate<Level, BlockPos> source) {
        clientSource = source;
    }

    public static void setClientCoverage(BiPredicate<Level, BlockPos> coverage) {
        clientCoverage = coverage;
    }

    public static boolean isKnown(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            return true;
        }
        TrackwrightSettings settings = Sides.forLevel(level);
        if (settings == null || !settings.unbreakableMarkers().enabled()) {
            return true;
        }
        BiPredicate<Level, BlockPos> coverage = clientCoverage;
        return coverage != null && coverage.test(level, pos);
    }

    public static boolean isProtected(Level level, BlockPos pos) {
        TrackwrightSettings settings = Sides.forLevel(level);
        if (settings == null || !settings.unbreakableMarkers().enabled()) {
            return false;
        }
        if (level.isClientSide()) {
            BiPredicate<Level, BlockPos> source = clientSource;
            return source != null && source.test(level, pos);
        }
        return isMarked(level, pos);
    }

    public static boolean isMarked(Level level, BlockPos pos) {
        MarkerSet set = setAt(level, pos.getX() >> 4, pos.getZ() >> 4);
        return set != null && set.contains(MarkerSet.cell(pos));
    }

    public static @Nullable MarkerSet setAt(Level level, int chunkX, int chunkZ) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        return chunk == null ? null : chunk.getExistingDataOrNull(ATTACHMENT.get());
    }

    public static boolean isLoaded(Level level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    public static int change(ServerLevel level, Collection<BlockPos> cells, boolean marked) {
        Long2ObjectOpenHashMap<IntOpenHashSet> byChunk = new Long2ObjectOpenHashMap<>();
        for (BlockPos pos : cells) {
            byChunk.computeIfAbsent(ChunkPos.asLong(pos), key -> new IntOpenHashSet()).add(MarkerSet.cell(pos));
        }
        AttachmentType<MarkerSet> type = ATTACHMENT.get();
        int changed = 0;
        IntOpenHashSet none = new IntOpenHashSet();
        for (Long2ObjectMap.Entry<IntOpenHashSet> entry : byChunk.long2ObjectEntrySet()) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(entry.getLongKey()), ChunkPos.getZ(entry.getLongKey()));
            if (chunk == null) {
                continue;
            }
            MarkerSet existing = chunk.getExistingDataOrNull(type);
            MarkerSet before = existing == null ? MarkerSet.EMPTY : existing;
            int delta = 0;
            for (int cell : entry.getValue()) {
                if (before.contains(cell) != marked) {
                    delta++;
                }
            }
            if (delta == 0) {
                continue;
            }
            MarkerSet after = marked ? before.with(entry.getValue(), none) : before.with(none, entry.getValue());
            if (after.isEmpty()) {
                chunk.removeData(type);
            } else {
                chunk.setData(type, after);
            }
            chunk.setUnsaved(true);
            changed += delta;
        }
        return changed;
    }

    public static boolean isMarkerTool(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TOOLS) && !stack.is(BlockMiner.TOOLS);
    }

    public static @Nullable Component refusal(MarkerMode mode, boolean allowed) {
        if (!mode.enabled()) {
            return SettingMessages.component(SettingMessages.MARKERS_OFF).withStyle(ChatFormatting.RED);
        }
        if (!allowed) {
            return SettingMessages.component(SettingMessages.MARKERS_OPERATORS_ONLY).withStyle(ChatFormatting.RED);
        }
        return null;
    }

    public static boolean mayChange(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (!player.mayBuild()) {
            return false;
        }
        return level.mayInteract(player, pos)
            && !player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())
            && BlockMiner.breakAllowed(level, player, pos, level.getBlockState(pos));
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        InteractionResult result = onUseBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (hand != InteractionHand.MAIN_HAND) {
            if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }
            Handled last = HANDLED.get(player.getUUID());
            long now = serverLevel.getServer().getTickCount();
            return last != null && now - last.tick() <= 1 && last.pos().equals(hit.getBlockPos()) ? InteractionResult.FAIL : InteractionResult.PASS;
        }
        if (!player.isSecondaryUseActive() || !isMarkerTool(player.getMainHandItem())) {
            return InteractionResult.PASS;
        }
        TrackwrightSettings settings = Sides.forLevel(level);
        if (settings == null || !settings.unbreakableMarkers().enabled()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos().immutable();
        long now = serverLevel.getServer().getTickCount();
        Handled previous = HANDLED.put(serverPlayer.getUUID(), new Handled(now, pos));
        if (previous != null && previous.pos().equals(pos) && now - previous.tick() <= HOLD_REPEAT) {
            return InteractionResult.SUCCESS;
        }
        MarkerMode mode = ServerConfig.get().unbreakableMarkers();
        Component refused = refusal(mode, mode.allows(serverPlayer));
        if (refused == null && !mayChange(serverLevel, serverPlayer, pos)) {
            refused = MarkerMessages.refusal(MarkerMessages.PROTECTED);
        }
        if (refused != null) {
            serverPlayer.displayClientMessage(refused, true);
            return InteractionResult.FAIL;
        }
        boolean mark = !isMarked(serverLevel, pos);
        change(serverLevel, List.of(pos), mark);
        serverPlayer.displayClientMessage(MarkerMessages.text(mark ? MarkerMessages.MARKED : MarkerMessages.UNMARKED), true);
        return InteractionResult.SUCCESS;
    }
}
