package dev.chaevsfe.trackwright.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class PlanGeometry {
    private static final int RADIUS_SAMPLES = 64;
    private static final int CLIMB_SAMPLES = 128;
    private static final double EPSILON = 1.0E-6;

    private PlanGeometry() {
    }

    public record Climb(double rise, double run, double steepest) {
    }

    public static Climb climb(@Nullable BezierConnection curve, int dx, int dy, int dz) {
        if (curve == null) {
            double run = Math.sqrt((double) dx * dx + (double) dz * dz);
            return new Climb(dy, run, run < EPSILON ? 0 : Math.abs(dy) / run);
        }
        Vec3 start = curve.starts.getFirst();
        Vec3 end = curve.starts.getSecond();
        double handle = curve.getHandleLength();
        Vec3 control1 = start.add(curve.axes.getFirst().scale(handle));
        Vec3 control2 = end.add(curve.axes.getSecond().scale(handle));
        double steepest = 0;
        double run = 0;
        Vec3 previous = start;
        for (int i = 0; i <= CLIMB_SAMPLES; i++) {
            float t = i / (float) CLIMB_SAMPLES;
            Vec3 direction = VecHelper.bezierDerivative(start, end, control1, control2, t);
            double across = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            if (across > EPSILON) {
                steepest = Math.max(steepest, Math.abs(direction.y) / across);
            }
            Vec3 point = VecHelper.bezier(start, end, control1, control2, t);
            run += Math.sqrt((point.x - previous.x) * (point.x - previous.x) + (point.z - previous.z) * (point.z - previous.z));
            previous = point;
        }
        return new Climb(end.y - start.y, run, steepest);
    }

    public static double headingChange(@Nullable Vec3 axis1, @Nullable Vec3 axis2) {
        Vec3 heading1 = horizontalUnit(axis1);
        Vec3 heading2 = horizontalUnit(axis2 == null ? null : axis2.scale(-1));
        if (heading1 == null || heading2 == null) {
            return Double.NaN;
        }
        double change = Math.toDegrees(Math.atan2(heading2.z, heading2.x) - Math.atan2(heading1.z, heading1.x));
        return ((change % 360) + 540) % 360 - 180;
    }

    public static double sideways(@Nullable Vec3 axis1, double dx, double dz) {
        Vec3 heading = horizontalUnit(axis1);
        if (heading == null) {
            return Double.NaN;
        }
        return heading.x * dz - heading.z * dx;
    }

    public static double tightestRadius(BezierConnection curve) {
        Vec3 p0 = curve.starts.getFirst();
        Vec3 p3 = curve.starts.getSecond();
        double handle = curve.getHandleLength();
        Vec3 p1 = p0.add(curve.axes.getFirst().scale(handle));
        Vec3 p2 = p3.add(curve.axes.getSecond().scale(handle));
        double sharpest = 0;
        for (int i = 0; i <= RADIUS_SAMPLES; i++) {
            double t = i / (double) RADIUS_SAMPLES;
            double u = 1 - t;
            double dx = 3 * u * u * (p1.x - p0.x) + 6 * u * t * (p2.x - p1.x) + 3 * t * t * (p3.x - p2.x);
            double dz = 3 * u * u * (p1.z - p0.z) + 6 * u * t * (p2.z - p1.z) + 3 * t * t * (p3.z - p2.z);
            double ddx = 6 * u * (p2.x - 2 * p1.x + p0.x) + 6 * t * (p3.x - 2 * p2.x + p1.x);
            double ddz = 6 * u * (p2.z - 2 * p1.z + p0.z) + 6 * t * (p3.z - 2 * p2.z + p1.z);
            double speed = Math.sqrt(dx * dx + dz * dz);
            if (speed < EPSILON) {
                continue;
            }
            sharpest = Math.max(sharpest, Math.abs(dx * ddz - dz * ddx) / (speed * speed * speed));
        }
        return sharpest < EPSILON ? Double.POSITIVE_INFINITY : 1 / sharpest;
    }

    private static @Nullable Vec3 horizontalUnit(@Nullable Vec3 vector) {
        if (vector == null) {
            return null;
        }
        double length = Math.sqrt(vector.x * vector.x + vector.z * vector.z);
        if (length < EPSILON) {
            return null;
        }
        return new Vec3(vector.x / length, 0, vector.z / length);
    }
}
