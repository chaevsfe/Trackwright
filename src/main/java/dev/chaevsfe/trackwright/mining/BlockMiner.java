package dev.chaevsfe.trackwright.mining;

import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.track.Messages;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class BlockMiner {
    public static final TagKey<Block> IMMUNE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "bore_immune"));
    public static final TagKey<Item> TOOLS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "bore_tools"));

    private static final double DROP_REACH = 0.5;

    public enum Refusal {
        NOT_LOADED(Messages.BORE_NOT_LOADED),
        UNBREAKABLE(Messages.BORE_UNBREAKABLE),
        BLOCK_ENTITY(Messages.BORE_BLOCK_ENTITY),
        IMMUNE(Messages.BORE_IMMUNE),
        TOO_HARD(Messages.BORE_TOO_HARD),
        WRONG_TOOL(Messages.BORE_WRONG_TOOL),
        TOOL_WORN(Messages.BORE_TOOL_WORN),
        PROTECTED(Messages.BORE_PROTECTED),
        LAVA(Messages.BORE_LAVA),
        FALLING(Messages.BORE_FALLING);

        private final String message;

        Refusal(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    public record Problem(Refusal refusal, BlockPos pos, BlockState state) {
    }

    public record Rules(boolean lavaCheck, double maxHardness) {
    }

    public record Outcome(int mined, @Nullable BlockPos stoppedAt) {
    }

    private BlockMiner() {
    }

    public static boolean isTool(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TOOLS);
    }

    public static boolean needsMining(BlockState state) {
        return !state.canBeReplaced();
    }

    public static @Nullable Problem checkBlock(Level level, BlockPos pos, BlockState state, ItemStack tool, boolean creative, Rules rules) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) {
            return new Problem(Refusal.UNBREAKABLE, pos, state);
        }
        if (state.hasBlockEntity()) {
            return new Problem(Refusal.BLOCK_ENTITY, pos, state);
        }
        if (state.is(IMMUNE)) {
            return new Problem(Refusal.IMMUNE, pos, state);
        }
        if (rules.maxHardness() > 0 && hardness > rules.maxHardness()) {
            return new Problem(Refusal.TOO_HARD, pos, state);
        }
        if (!creative && state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) {
            return new Problem(Refusal.WRONG_TOOL, pos, state);
        }
        return null;
    }

    public static int wear(Level level, ItemStack tool, BlockPos pos, BlockState state) {
        Tool component = tool.get(DataComponents.TOOL);
        if (component == null || !tool.isDamageableItem() || state.getDestroySpeed(level, pos) == 0) {
            return 0;
        }
        return Math.max(0, component.damagePerBlock());
    }

    public static int usesLeft(ItemStack tool) {
        return tool.isDamageableItem() ? tool.getMaxDamage() - tool.getDamageValue() : -1;
    }

    public static @Nullable Problem checkSurroundings(Level level, Collection<BlockPos> cells, Rules rules) {
        Set<BlockPos> mined = new HashSet<>(cells);
        for (BlockPos pos : cells) {
            BlockPos above = pos.above();
            if (!mined.contains(above)) {
                if (!level.isLoaded(above)) {
                    if (!level.isClientSide()) {
                        return new Problem(Refusal.NOT_LOADED, above, level.getBlockState(pos));
                    }
                } else {
                    BlockState state = level.getBlockState(above);
                    if (state.getBlock() instanceof Fallable) {
                        return new Problem(Refusal.FALLING, above.immutable(), state);
                    }
                }
            }
            if (!rules.lavaCheck()) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (mined.contains(next)) {
                    continue;
                }
                if (!level.isLoaded(next)) {
                    if (!level.isClientSide()) {
                        return new Problem(Refusal.NOT_LOADED, next, level.getBlockState(pos));
                    }
                    continue;
                }
                if (level.getFluidState(next).is(FluidTags.LAVA)) {
                    return new Problem(Refusal.LAVA, next.immutable(), level.getBlockState(next));
                }
            }
        }
        return null;
    }

    public static @Nullable Problem checkPermission(ServerLevel level, ServerPlayer player, List<BlockPos> cells) {
        if (cells.isEmpty()) {
            return null;
        }
        if (!player.mayBuild()) {
            BlockPos first = cells.getFirst();
            return new Problem(Refusal.PROTECTED, first, level.getBlockState(first));
        }
        return withToolInMainHand(player, () -> {
            for (BlockPos pos : cells) {
                BlockState state = level.getBlockState(pos);
                if (!level.mayInteract(player, pos)
                    || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())
                    || !PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, null)) {
                    return new Problem(Refusal.PROTECTED, pos, state);
                }
            }
            return null;
        });
    }

    public static Outcome mine(ServerLevel level, ServerPlayer player, List<BlockPos> cells) {
        Set<ItemEntity> drops = new LinkedHashSet<>();
        Outcome outcome = withToolInMainHand(player, () -> {
            boolean hadTool = !player.getMainHandItem().isEmpty();
            int mined = 0;
            for (BlockPos pos : cells) {
                BlockState before = level.getBlockState(pos);
                if (!needsMining(before)) {
                    continue;
                }
                if (hadTool && player.getMainHandItem().isEmpty()) {
                    return new Outcome(mined, pos);
                }
                AABB reach = new AABB(pos).inflate(DROP_REACH);
                Set<ItemEntity> present = new HashSet<>(level.getEntitiesOfClass(ItemEntity.class, reach));
                boolean destroyed = player.gameMode.destroyBlock(pos);
                if (!destroyed && level.getBlockState(pos) == before) {
                    return new Outcome(mined, pos);
                }
                mined++;
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, reach)) {
                    if (item.isAlive() && !present.contains(item)) {
                        drops.add(item);
                    }
                }
            }
            return new Outcome(mined, null);
        });
        for (ItemEntity item : drops) {
            if (!item.isAlive()) {
                continue;
            }
            ItemStack stack = item.getItem().copy();
            item.discard();
            player.getInventory().placeItemBackInInventory(stack);
        }
        return outcome;
    }

    private static <T> T withToolInMainHand(ServerPlayer player, Supplier<T> action) {
        ItemStack main = player.getMainHandItem();
        ItemStack tool = player.getOffhandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        try {
            return action.get();
        } finally {
            ItemStack used = player.getMainHandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, main);
            player.setItemInHand(InteractionHand.OFF_HAND, used);
        }
    }
}
