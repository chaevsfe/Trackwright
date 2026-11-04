package dev.chaevsfe.trackwright.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackBlockItem;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import com.zurrtum.create.content.trains.track.TrackPlacement;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.zurrtum.create.infrastructure.component.ConnectingFrom;
import dev.chaevsfe.trackwright.track.QuarterCircle;
import dev.chaevsfe.trackwright.track.TrackItems;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

public final class RadiusLock {
    public static final String KEY_NAME = "trackwright.key.lock_radius";
    public static final String OFF_LINE = "trackwright.radius_off_line";
    public static final String TOO_CLOSE = "trackwright.radius_too_close";
    private static final int FALLBACK_REACH = 32;

    private static @Nullable KeyMapping key;
    private static boolean on;
    private static int corner;
    private static boolean pressed;
    private static boolean scrolled;
    private static boolean ctrlForClick;
    private static @Nullable Judgement last;

    private record Judgement(PlacementInfo source, PlacementInfo shown, int corner, boolean maximise, QuarterCircle.Judged judged) {
    }

    private RadiusLock() {
    }

    public static void register(KeyMapping.Category category) {
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY_NAME, InputConstants.KEY_R, category));
    }

    public static boolean on() {
        return on;
    }

    public static boolean bound() {
        KeyMapping mapping = key;
        return mapping != null && !mapping.isUnbound();
    }

    public static void unlock() {
        if (on) {
            on = false;
            TrackGuide.forgetPlan();
        }
    }

    public static void reset() {
        ctrlForClick = false;
        pressed = false;
        scrolled = false;
        last = null;
    }

    public static void tick(Minecraft mc) {
        ctrlForClick = false;
        KeyMapping mapping = key;
        if (mapping == null) {
            return;
        }
        boolean down = mapping.isDown();
        boolean clicked = false;
        while (mapping.consumeClick()) {
            clicked = true;
        }
        if ((clicked || down) && !pressed) {
            pressed = true;
            scrolled = false;
        }
        if (pressed && !down) {
            pressed = false;
            if (!scrolled) {
                tap(mc);
            }
        }
        LocalPlayer player = mc.player;
        if (player != null && on) {
            RadiusTargets.show(mc, player);
        }
    }

    private static void tap(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.screen() != null) {
            return;
        }
        ItemStack stack = heldTrack(player);
        if (stack == null) {
            return;
        }
        if (on) {
            on = false;
            say(player, Component.translatable("trackwright.radius.off"));
            TrackGuide.forgetPlan();
            return;
        }
        int aimed = aimedCorner(player, stack);
        int next = aimed > 0 ? aimed : corner > 0 ? corner : smallest(stack);
        turnOn(player, clamp(next, stack), "trackwright.radius.on");
    }

    private static void turnOn(LocalPlayer player, int next, String key) {
        boolean widest = WidestTurnLock.locked();
        if (widest) {
            WidestTurnLock.unlock();
        }
        on = true;
        corner = next;
        String number = number(QuarterCircle.radius(next));
        say(player, Component.translatable(widest ? "trackwright.radius.on_widest_off" : key, white(number)));
        TrackGuide.forgetPlan();
    }

    public static boolean onScroll(int wheel) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        KeyMapping mapping = key;
        if (wheel == 0 || mapping == null || player == null || mc.gui.screen() != null || !mapping.isDown()) {
            return false;
        }
        ItemStack stack = heldTrack(player);
        if (stack == null) {
            return false;
        }
        pressed = true;
        scrolled = true;
        int base = corner > 0 ? clamp(corner, stack) : smallest(stack);
        int next = base + Integer.signum(wheel);
        if (next < smallest(stack)) {
            if (!on) {
                turnOn(player, base, "trackwright.radius.changed");
            }
            say(player, Component.translatable("trackwright.radius.smallest", white(number(QuarterCircle.radius(base)))).withStyle(ChatFormatting.YELLOW));
            return true;
        }
        if (next > largest()) {
            if (!on) {
                turnOn(player, base, "trackwright.radius.changed");
            }
            say(player, Component.translatable("trackwright.radius.largest", white(number(QuarterCircle.radius(base)))).withStyle(ChatFormatting.YELLOW));
            return true;
        }
        turnOn(player, next, "trackwright.radius.changed");
        return true;
    }

    public static boolean needsCtrl() {
        return ctrlForClick;
    }

    public static boolean beforeUseItem(Minecraft mc, LocalPlayer player) {
        ctrlForClick = false;
        if (!on || player.isShiftKeyDown() || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null || !TrackItems.isTrack(stack) || !(stack.getItem() instanceof TrackBlockItem item)) {
            return false;
        }
        Level level = player.level();
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ITrackBlock)) {
            if (!state.canBeReplaced()) {
                pos = pos.relative(hit.getDirection());
            }
            state = item.getPlacementState(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            if (state == null) {
                return false;
            }
        }
        boolean ctrl = mc.options.keySprint.isDown();
        QuarterCircle.Judged judged = judgeFresh(level, player, from, pos, state, stack, ctrl);
        if (judged.verdict().refused()) {
            String message = judged.verdict() == QuarterCircle.Verdict.TOO_CLOSE ? TOO_CLOSE : OFF_LINE;
            player.sendOverlayMessage(Component.translatable("create.track." + message).withStyle(ChatFormatting.RED));
            AllSoundEvents.DENY.play(level, player, player.blockPosition(), 1, 1);
            return true;
        }
        ctrlForClick = judged.verdict() == QuarterCircle.Verdict.EXACT_WITH_CTRL && !ctrl;
        return false;
    }

    private static QuarterCircle.Judged judgeFresh(Level level, LocalPlayer player, ConnectingFrom from, BlockPos pos, BlockState state, ItemStack stack,
                                                   boolean ctrl) {
        int effective = effectiveCorner(stack);
        BlockPos target = pos.immutable();
        BlockState placed = state;
        PlacementCache saved = PlacementCache.save();
        try {
            return QuarterCircle.judge(maximise -> {
                TrackPlacement.cached = null;
                return TrackPlacement.tryConnect(level, player, target, placed, stack, false, maximise);
            }, from.pos(), target, effective, ctrl);
        } finally {
            saved.restore();
            TrackPlacement.cached = null;
        }
    }

    public static PlacementInfo forGuide(Level level, LocalPlayer player, BlockPos target, BlockState state, ItemStack stack, boolean maximise,
                                         PlacementInfo info) {
        if (!on || stack != player.getMainHandItem()) {
            last = null;
            return info;
        }
        ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null) {
            last = null;
            return info;
        }
        int effective = effectiveCorner(stack);
        Judgement previous = last;
        if (previous != null && previous.source() == info && previous.corner() == effective && previous.maximise() == maximise) {
            return previous.shown();
        }
        BlockPos at = target.immutable();
        QuarterCircle.Judged judged = QuarterCircle.judge(wide -> {
            if (wide == maximise) {
                return info;
            }
            PlacementCache saved = PlacementCache.save();
            try {
                return TrackPlacement.tryConnect(level, player, at, state, stack, false, wide);
            } finally {
                saved.restore();
            }
        }, from.pos(), at, effective, maximise);
        PlacementInfo shown = info;
        if (judged.verdict() == QuarterCircle.Verdict.EXACT_WITH_CTRL && judged.laid() != null) {
            shown = judged.laid();
        } else if (judged.verdict().refused()) {
            info.valid = false;
            info.withMessage(judged.verdict() == QuarterCircle.Verdict.TOO_CLOSE ? TOO_CLOSE : OFF_LINE);
        }
        last = new Judgement(info, shown, effective, maximise, judged);
        return shown;
    }

    public static QuarterCircle.@Nullable Judged judgedFor(PlacementInfo info) {
        Judgement current = last;
        return on && current != null && current.shown() == info ? current.judged() : null;
    }

    public static int corner() {
        return corner;
    }

    public static int effectiveCorner(ItemStack stack) {
        int smallest = smallest(stack);
        int chosen = corner > 0 ? corner : smallest;
        return Math.min(Math.max(chosen, smallest), Math.max(smallest, largest()));
    }

    public static boolean raised(ItemStack stack) {
        return corner > 0 && corner < smallest(stack);
    }

    public static int smallest(ItemStack stack) {
        return QuarterCircle.smallestCorner(TrackMaterial.fromItem(stack.getItem()));
    }

    public static int largest() {
        Integer limit = GuideLines.connectionLimit();
        return QuarterCircle.largestCorner(limit == null ? FALLBACK_REACH : limit);
    }

    public static int reach() {
        Integer limit = GuideLines.connectionLimit();
        return limit == null ? FALLBACK_REACH : limit;
    }

    private static int clamp(int value, ItemStack stack) {
        return Math.max(smallest(stack), Math.min(Math.max(smallest(stack), largest()), value));
    }

    private static int aimedCorner(LocalPlayer player, ItemStack stack) {
        ConnectingFrom from = stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (from == null) {
            return 0;
        }
        PlacementInfo info = TrackGuide.plannedFrom(from);
        BlockPos target = TrackGuide.plannedTarget(from);
        if (info == null || target == null || !info.valid) {
            return 0;
        }
        QuarterCircle.Square square = QuarterCircle.square(info, from.pos(), target);
        return square == null ? 0 : Math.max(0, QuarterCircle.laidCorner(info, square));
    }

    static @Nullable ItemStack heldTrack(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return TrackItems.isTrack(stack) && stack.getItem() instanceof TrackBlockItem ? stack : null;
    }

    static Component white(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    static String number(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.05) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void say(LocalPlayer player, Component message) {
        player.sendOverlayMessage(message);
        ActionBarHold.start();
    }
}
