package dev.chaevsfe.trackwright.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class QuarterCircle {
    private static final double EPSILON = 1.0E-6;
    private static final double MIN_TURN = 7;
    private static final double TURN_MARGIN = 0.6;

    private QuarterCircle() {
    }

    public enum Verdict {
        NOT_SHAPED,
        EXACT_PLAIN,
        EXACT_WITH_CTRL,
        TOO_CLOSE,
        OFF_LINE;

        public boolean refused() {
            return this == TOO_CLOSE || this == OFF_LINE;
        }

        public boolean exact() {
            return this == EXACT_PLAIN || this == EXACT_WITH_CTRL;
        }
    }

    public record Square(int ahead, int side) {
    }

    public record Judged(Verdict verdict, int corner, @Nullable Square square, @Nullable PlacementInfo laid) {
        public static final Judged NONE = new Judged(Verdict.NOT_SHAPED, 0, null, null);
    }

    @FunctionalInterface
    public interface Planner {
        @Nullable PlacementInfo plan(boolean maximise);
    }

    public static double radius(int corner) {
        return corner - 0.5;
    }

    public static int smallestCorner(@Nullable TrackMaterial material) {
        return (int) Math.ceil(MIN_TURN * BoreProfiles.gaugeWidth(material) + TURN_MARGIN - EPSILON);
    }

    public static int largestCorner(int reach) {
        return Math.max(1, (int) Math.floor(reach / Math.sqrt(2) + EPSILON));
    }

    public static boolean gridUnit(@Nullable Vec3 axis) {
        if (axis == null || Math.abs(axis.y) > EPSILON) {
            return false;
        }
        double x = Math.abs(axis.x);
        double z = Math.abs(axis.z);
        return Math.abs(x - 1) < EPSILON && z < EPSILON || Math.abs(z - 1) < EPSILON && x < EPSILON;
    }

    public static boolean flat(@Nullable Vec3 normal) {
        return normal != null && Math.abs(normal.x) < EPSILON && Math.abs(normal.z) < EPSILON && normal.y > 0;
    }

    public static @Nullable Square square(PlacementInfo info, BlockPos from, BlockPos to) {
        BezierConnection curve = ((PlacementInfoExtension) info).trackwright$curve();
        if (curve == null) {
            return null;
        }
        Vec3 axis1 = curve.axes.getFirst();
        Vec3 axis2 = curve.axes.getSecond();
        if (!gridUnit(axis1) || !gridUnit(axis2) || Math.abs(axis1.dot(axis2)) > EPSILON
            || !flat(curve.normals.getFirst()) || !flat(curve.normals.getSecond())) {
            return null;
        }
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int ahead = (int) Math.round(Math.abs(dx * axis1.x + dz * axis1.z));
        int side = (int) Math.round(Math.abs(dx * axis2.x + dz * axis2.z));
        return new Square(ahead, side);
    }

    public static int laidCorner(PlacementInfo info, Square square) {
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        int first = square.ahead() - extension.trackwright$end1Extent();
        int second = square.side() - extension.trackwright$end2Extent();
        return first == second ? first : -1;
    }

    public static Judged judge(Planner planner, BlockPos from, BlockPos to, int corner, boolean maximise) {
        PlacementInfo laid = planner.plan(maximise);
        if (laid == null || !((PlacementInfoExtension) laid).trackwright$valid()) {
            return new Judged(Verdict.NOT_SHAPED, corner, null, laid);
        }
        Square square = square(laid, from, to);
        if (square == null) {
            return new Judged(Verdict.NOT_SHAPED, corner, null, laid);
        }
        if (laidCorner(laid, square) == corner) {
            return new Judged(Verdict.EXACT_PLAIN, corner, square, laid);
        }
        if (!maximise) {
            PlacementInfo widest = planner.plan(true);
            if (widest != null && ((PlacementInfoExtension) widest).trackwright$valid()) {
                Square wide = square(widest, from, to);
                if (wide != null && laidCorner(widest, wide) == corner) {
                    return new Judged(Verdict.EXACT_WITH_CTRL, corner, wide, widest);
                }
            }
        }
        Verdict verdict = square.ahead() < corner || square.side() < corner ? Verdict.TOO_CLOSE : Verdict.OFF_LINE;
        return new Judged(verdict, corner, square, laid);
    }
}
