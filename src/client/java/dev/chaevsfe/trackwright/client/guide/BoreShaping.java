package dev.chaevsfe.trackwright.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.zurrtum.create.AllDataComponents;
import com.zurrtum.create.content.trains.track.TrackMaterial;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.zurrtum.create.infrastructure.component.ConnectingFrom;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import dev.chaevsfe.trackwright.network.PlacementPayload;
import dev.chaevsfe.trackwright.track.BoreProfiles;
import dev.chaevsfe.trackwright.track.TrackItems;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BoreShaping {
    public static final String KEY_NAME = "trackwright.key.shape_bore";
    private static final double STEEP = 45;
    private static final double ACROSS = Math.cos(Math.toRadians(45));
    private static final double EPSILON = 1.0E-6;

    private static @Nullable KeyMapping key;
    private static @Nullable BoreProfile shaped;
    private static @Nullable Vec3 facing;
    private static boolean wasDown;

    public enum Target {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        ALONG
    }

    private BoreShaping() {
    }

    public static void register(KeyMapping.Category category) {
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping(KEY_NAME, InputConstants.KEY_LALT, category));
    }

    public static void tick(Minecraft mc) {
        boolean down = held();
        if (down && !wasDown && mc.player != null) {
            facing = Vec3.atLowerCornerOf(mc.player.getDirection().getUnitVec3i());
        } else if (!down) {
            facing = null;
        }
        wasDown = down;
    }

    public static void reset() {
        shaped = null;
        facing = null;
        wasDown = false;
    }

    public static boolean held() {
        KeyMapping mapping = key;
        return mapping != null && mapping.isDown();
    }

    public static BoreProfiles.Request request(Player player) {
        BoreProfile profile = shaped;
        return profile == null ? BoreProfiles.Request.NONE : new BoreProfiles.Request(profile.left(), profile.right(), profile.up());
    }

    public static boolean supported() {
        return RemoteSettings.get() != null && ClientPlayNetworking.canSend(PlacementPayload.TYPE);
    }

    public static void beforeUse(UseOnContext context) {
        if (!context.getLevel().isClientSide() || context.getHand() != InteractionHand.MAIN_HAND
            || !context.getItemInHand().has(AllDataComponents.TRACK_CONNECTING_FROM) || !ClientPlayNetworking.canSend(PlacementPayload.TYPE)) {
            return;
        }
        ClientPlayNetworking.send(PlacementPayload.of(shaped));
    }

    public static @Nullable ItemStack heldTrack(Player player) {
        ItemStack stack = player.getMainHandItem();
        return TrackItems.isTrack(stack) ? stack : null;
    }

    public static boolean boring(Player player) {
        TrackwrightSettings settings = RemoteSettings.get();
        return settings != null && heldTrack(player) != null && BlockMiner.isTool(player.getOffhandItem())
            && settings.boreThroughBlocks().allows(player);
    }

    public static @Nullable BoreProfile current(Player player) {
        TrackwrightSettings settings = RemoteSettings.get();
        ItemStack stack = heldTrack(player);
        if (settings == null || stack == null) {
            return null;
        }
        return BoreProfiles.request(player.level(), player).over(BoreProfiles.defaultFor(settings, TrackMaterial.fromItem(stack.getItem())))
            .clamp(settings.boreWidthMax(), settings.boreHeadroomMax());
    }

    public static boolean onScroll(int wheel) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (wheel == 0 || player == null || mc.gui.screen() != null || !held() || heldTrack(player) == null
            || !BlockMiner.isTool(player.getOffhandItem())) {
            return false;
        }
        TrackwrightSettings settings = RemoteSettings.get();
        BoreProfile current = current(player);
        if (settings == null || current == null || !ClientPlayNetworking.canSend(PlacementPayload.TYPE)) {
            say(player, Component.translatable("trackwright.shape.unsupported").withStyle(ChatFormatting.RED));
            return true;
        }
        Target target = target(mc, player);
        int step = Integer.signum(wheel);
        BoreProfile next = switch (target) {
            case LEFT -> current.withLeft(current.left() + step);
            case RIGHT -> current.withRight(current.right() + step);
            case UP -> current.withUp(current.up() + step);
            case DOWN, ALONG -> null;
        };
        if (next == null) {
            say(player, Component.translatable(target == Target.DOWN ? "trackwright.shape.no_down" : "trackwright.shape.look_across")
                .withStyle(ChatFormatting.YELLOW));
            return true;
        }
        boolean fits = next.left() >= 0 && next.right() >= 0 && next.up() >= 0
            && next.equals(next.clamp(settings.boreWidthMax(), settings.boreHeadroomMax()));
        if (!fits) {
            String limit = step > 0 ? "trackwright.shape.largest" : "trackwright.shape.smallest";
            say(player, Component.translatable(limit, GuideLines.profileLine(current)).withStyle(ChatFormatting.YELLOW));
            return true;
        }
        shaped = next;
        TrackGuide.forgetPlan();
        say(player, GuideLines.profileLine(next));
        return true;
    }

    public static List<Component> guideLines(Player player) {
        List<Component> lines = new ArrayList<>();
        if (!boring(player)) {
            return lines;
        }
        BoreProfile profile = current(player);
        if (profile != null) {
            lines.add(GuideLines.profileLine(profile));
        }
        if (!supported()) {
            return lines;
        }
        if (held()) {
            Target target = target(Minecraft.getInstance(), player);
            String key = switch (target) {
                case LEFT -> "trackwright.shape.target.left";
                case RIGHT -> "trackwright.shape.target.right";
                case UP -> "trackwright.shape.target.up";
                case DOWN -> "trackwright.shape.target.down";
                case ALONG -> "trackwright.shape.target.along";
            };
            lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("trackwright.shape.hint", Component.keybind(KEY_NAME).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    public static Target target(Minecraft mc, Player player) {
        float pitch = player.getXRot();
        if (pitch < -STEEP) {
            return Target.UP;
        }
        if (pitch > STEEP) {
            return Target.DOWN;
        }
        Vec3 tangent = tangent(mc, player);
        Vec3 look = player.getLookAngle();
        double lx = look.x;
        double lz = look.z;
        double length = Math.sqrt(lx * lx + lz * lz);
        if (tangent == null || length < EPSILON) {
            return Target.ALONG;
        }
        lx /= length;
        lz /= length;
        if (Math.abs(lx * tangent.x + lz * tangent.z) >= ACROSS - EPSILON) {
            return Target.ALONG;
        }
        double side = lx * -tangent.z + lz * tangent.x;
        return side > 0 ? Target.RIGHT : Target.LEFT;
    }

    private static @Nullable Vec3 tangent(Minecraft mc, Player player) {
        ItemStack stack = heldTrack(player);
        ConnectingFrom from = stack == null ? null : stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        Vec3 direction;
        if (from != null) {
            PlacementInfo info = TrackGuide.plannedFrom(from);
            if (info != null && info.axis1 != null) {
                direction = info.axis1;
            } else {
                direction = from.axis();
                HitResult hit = mc.hitResult;
                Vec3 aim = hit != null && hit.getType() != HitResult.Type.MISS ? hit.getLocation() : player.getEyePosition().add(player.getLookAngle().scale(8));
                Vec3 toward = aim.subtract(Vec3.atCenterOf(from.pos()));
                if (direction.x * toward.x + direction.z * toward.z < 0) {
                    direction = direction.scale(-1);
                }
            }
        } else {
            direction = facing != null ? facing : Vec3.atLowerCornerOf(player.getDirection().getUnitVec3i());
        }
        double length = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return length < EPSILON ? null : new Vec3(direction.x / length, 0, direction.z / length);
    }

    private static void say(LocalPlayer player, Component message) {
        player.sendOverlayMessage(message);
        ActionBarHold.start();
    }
}
