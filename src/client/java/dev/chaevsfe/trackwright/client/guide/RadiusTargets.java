package dev.chaevsfe.trackwright.client.guide;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackPlacement.ConnectingFrom;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import dev.chaevsfe.trackwright.track.QuarterCircle;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RadiusTargets {
    private static final String CORNER = "trackwright_radius_corner_";
    private static final String RAY = "trackwright_radius_ray_";
    private static final int COLOR = 0x95CD41;
    private static final double LIFT = 0.1;

    private RadiusTargets() {
    }

    public static void show(Minecraft mc, LocalPlayer player) {
        ItemStack stack = RadiusLock.heldTrack(player);
        ConnectingFrom from = stack == null ? null : stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
        if (stack == null || from == null || !QuarterCircle.gridUnit(from.axis()) || !QuarterCircle.flat(from.normal())) {
            return;
        }
        Vec3 ahead = ahead(mc, player, from);
        int corner = RadiusLock.effectiveCorner(stack);
        BlockPos start = from.pos();
        int ax = (int) Math.round(ahead.x);
        int az = (int) Math.round(ahead.z);
        long reach = RadiusLock.reach();
        int extra = (int) Math.floor(Math.sqrt(Math.max(0, reach * reach - (long) corner * corner)) + 1.0E-6) - corner;
        showSide(start, ax, az, -az, ax, corner, extra, "right");
        showSide(start, ax, az, az, -ax, corner, extra, "left");
    }

    private static void showSide(BlockPos start, int ax, int az, int sx, int sz, int corner, int extra, String name) {
        BlockPos cell = start.offset(ax * corner + sx * corner, 0, az * corner + sz * corner);
        Outliner.getInstance().showAABB(CORNER + name, new AABB(cell).inflate(1 / 64.0))
            .colored(COLOR)
            .disableLineNormals()
            .lineWidth(1 / 16f);
        if (extra < 1) {
            Outliner.getInstance().remove(RAY + name + "_ahead");
            Outliner.getInstance().remove(RAY + name + "_side");
            return;
        }
        ray(RAY + name + "_ahead", cell.offset(ax, 0, az), cell.offset(ax * extra, 0, az * extra));
        ray(RAY + name + "_side", cell.offset(sx, 0, sz), cell.offset(sx * extra, 0, sz * extra));
    }

    private static void ray(String key, BlockPos first, BlockPos last) {
        Vec3 from = new Vec3(first.getX() + 0.5, first.getY() + LIFT, first.getZ() + 0.5);
        Vec3 to = new Vec3(last.getX() + 0.5, last.getY() + LIFT, last.getZ() + 0.5);
        Outliner.getInstance().showLine(key, from, to)
            .colored(COLOR)
            .disableLineNormals()
            .lineWidth(1 / 16f);
    }

    private static Vec3 ahead(Minecraft mc, LocalPlayer player, ConnectingFrom from) {
        PlacementInfo info = TrackGuide.plannedFrom(from);
        if (info != null && ((PlacementInfoExtension) info).trackwright$valid() && QuarterCircle.gridUnit(axisOf(info))) {
            return axisOf(info);
        }
        Vec3 axis = from.axis();
        HitResult hit = mc.hitResult;
        Vec3 aim = hit != null && hit.getType() != HitResult.Type.MISS ? hit.getLocation() : player.getEyePosition().add(player.getLookAngle().scale(8));
        Vec3 toward = aim.subtract(Vec3.atCenterOf(from.pos()));
        return axis.x * toward.x + axis.z * toward.z < 0 ? axis.scale(-1) : axis;
    }

    private static @Nullable Vec3 axisOf(PlacementInfo info) {
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        BezierConnection curve = extension.trackwright$curve();
        return curve != null ? curve.axes.getFirst() : extension.trackwright$axis1();
    }
}
