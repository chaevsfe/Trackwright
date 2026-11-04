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
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
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
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

public final class Markers {
    public static final TagKey<Item> TOOLS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "marker_tools"));
    public static final AttachmentType<MarkerSet> ATTACHMENT = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "markers"),
        builder -> builder.persistent(MarkerSet.CODEC)
    );
    public static final int BOX_CAP = 32768;

    private static final Map<ResourceKey<Level>, Long> GENERATIONS = new ConcurrentHashMap<>();
    private static volatile @Nullable BiPredicate<Level, BlockPos> clientSource;

    private Markers() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(Markers::onUseBlock);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> MarkerCommands.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(MarkerSync::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> MarkerSync.forget(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            MarkerSync.clearAll();
            GENERATIONS.clear();
        });
    }

    public static void setClientSource(BiPredicate<Level, BlockPos> source) {
        clientSource = source;
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
        return chunk == null ? null : chunk.getAttached(ATTACHMENT);
    }

    public static boolean isLoaded(Level level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    public static int change(ServerLevel level, Collection<BlockPos> cells, boolean marked) {
        Long2ObjectOpenHashMap<IntOpenHashSet> byChunk = new Long2ObjectOpenHashMap<>();
        for (BlockPos pos : cells) {
            byChunk.computeIfAbsent(ChunkPos.pack(pos), key -> new IntOpenHashSet()).add(MarkerSet.cell(pos));
        }
        int changed = 0;
        IntOpenHashSet none = new IntOpenHashSet();
        for (Long2ObjectMap.Entry<IntOpenHashSet> entry : byChunk.long2ObjectEntrySet()) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(entry.getLongKey()), ChunkPos.getZ(entry.getLongKey()));
            if (chunk == null) {
                continue;
            }
            MarkerSet before = chunk.getAttachedOrElse(ATTACHMENT, MarkerSet.EMPTY);
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
                chunk.removeAttached(ATTACHMENT);
            } else {
                chunk.setAttached(ATTACHMENT, after);
            }
            chunk.markUnsaved();
            changed += delta;
        }
        if (changed > 0) {
            GENERATIONS.merge(level.dimension(), 1L, Long::sum);
        }
        return changed;
    }

    public static long generation(ResourceKey<Level> dimension) {
        return GENERATIONS.getOrDefault(dimension, 0L);
    }

    public static boolean isMarkerTool(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TOOLS) && !stack.is(BlockMiner.TOOLS);
    }

    public static @Nullable Component refusal(MarkerMode mode, PermissionSet permissions) {
        if (!mode.enabled()) {
            return SettingMessages.component(SettingMessages.MARKERS_OFF).withStyle(ChatFormatting.RED);
        }
        if (!mode.allows(permissions)) {
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
            && PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, level.getBlockState(pos), level.getBlockEntity(pos));
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !player.isSecondaryUseActive() || !isMarkerTool(player.getMainHandItem())) {
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
        Component refused = refusal(ServerConfig.get().unbreakableMarkers(), serverPlayer.permissions());
        if (refused == null && !mayChange(serverLevel, serverPlayer, pos)) {
            refused = MarkerMessages.refusal(MarkerMessages.PROTECTED);
        }
        if (refused != null) {
            serverPlayer.sendOverlayMessage(refused);
            return InteractionResult.FAIL;
        }
        boolean mark = !isMarked(serverLevel, pos);
        change(serverLevel, List.of(pos), mark);
        serverPlayer.sendOverlayMessage(MarkerMessages.text(mark ? MarkerMessages.MARKED : MarkerMessages.UNMARKED));
        return InteractionResult.SUCCESS;
    }
}
