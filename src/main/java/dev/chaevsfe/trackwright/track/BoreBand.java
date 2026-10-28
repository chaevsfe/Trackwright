package dev.chaevsfe.trackwright.track;

import dev.chaevsfe.trackwright.config.BoreProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BoreBand {
    private static final double END_REACH = Math.sqrt(0.5);
    private static final double EPSILON = 1.0E-6;

    private BoreBand() {
    }

    public record Seed(BlockPos cell, double tx, double tz) {
        public static @Nullable Seed of(BlockPos cell, @Nullable Vec3 direction) {
            if (direction == null) {
                return null;
            }
            double length = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            if (length < EPSILON) {
                return null;
            }
            return new Seed(cell.immutable(), direction.x / length, direction.z / length);
        }

        public double rightX() {
            return -tz;
        }

        public double rightZ() {
            return tx;
        }
    }

    public record Line(List<BlockPos> cells, List<Vec3> points) {
        public Line {
            cells = List.copyOf(cells);
            points = List.copyOf(points);
        }
    }

    public static final class Builder {
        private final List<BlockPos> cells = new ArrayList<>();
        private final List<Vec3> points = new ArrayList<>();

        public Builder cell(BlockPos cell) {
            cells.add(cell.immutable());
            return this;
        }

        public Builder cells(Iterable<BlockPos> list) {
            for (BlockPos cell : list) {
                cell(cell);
            }
            return this;
        }

        public Builder point(Vec3 point) {
            Vec3 flat = new Vec3(point.x, 0, point.z);
            if (points.isEmpty() || points.getLast().distanceToSqr(flat) > EPSILON) {
                points.add(flat);
            }
            return this;
        }

        public Builder centre(BlockPos cell) {
            return point(new Vec3(cell.getX() + 0.5, 0, cell.getZ() + 0.5));
        }

        public Line build() {
            return new Line(cells, points);
        }
    }

    public static Map<BlockPos, List<BlockPos>> sides(Line line, BoreProfile profile) {
        Map<BlockPos, List<BlockPos>> found = new LinkedHashMap<>();
        for (BlockPos cell : line.cells()) {
            found.putIfAbsent(cell, new ArrayList<>());
        }
        List<Vec3> points = line.points();
        if (points.size() < 2 || line.cells().isEmpty() || profile.left() == 0 && profile.right() == 0) {
            return found;
        }
        double leftEdge = -(profile.left() + 0.5);
        double rightEdge = profile.right() + 0.5;
        int reach = Math.max(profile.left(), profile.right()) + 1;
        Set<Long> columns = new HashSet<>();
        for (BlockPos cell : line.cells()) {
            columns.add(column(cell.getX(), cell.getZ()));
        }
        Set<Long> seen = new HashSet<>();
        for (BlockPos seed : line.cells()) {
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    int x = seed.getX() + dx;
                    int z = seed.getZ() + dz;
                    long key = column(x, z);
                    if (columns.contains(key) || !seen.add(key)) {
                        continue;
                    }
                    double side = side(points, x + 0.5, z + 0.5);
                    if (Double.isNaN(side) || side <= leftEdge + EPSILON || side >= rightEdge - EPSILON) {
                        continue;
                    }
                    BlockPos owner = nearest(line.cells(), x + 0.5, z + 0.5);
                    found.get(owner).add(new BlockPos(x, owner.getY(), z));
                }
            }
        }
        return found;
    }

    private static long column(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static BlockPos nearest(List<BlockPos> cells, double x, double z) {
        BlockPos best = cells.getFirst();
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos cell : cells) {
            double dx = cell.getX() + 0.5 - x;
            double dz = cell.getZ() + 0.5 - z;
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance - EPSILON) {
                bestDistance = distance;
                best = cell;
            }
        }
        return best;
    }

    private static double side(List<Vec3> points, double x, double z) {
        double best = Double.MAX_VALUE;
        double side = Double.NaN;
        int last = points.size() - 2;
        for (int i = 0; i <= last; i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            double ux = b.x - a.x;
            double uz = b.z - a.z;
            double length = Math.sqrt(ux * ux + uz * uz);
            if (length < EPSILON) {
                continue;
            }
            ux /= length;
            uz /= length;
            double along = (x - a.x) * ux + (z - a.z) * uz;
            double from = i == 0 ? -END_REACH : 0;
            double to = i == last ? length + END_REACH : length;
            if (along < from - EPSILON || along > to + EPSILON) {
                if (i == 0 && along < from || i == last && along > to) {
                    continue;
                }
            }
            double clamped = Math.max(from, Math.min(to, along));
            double px = a.x + ux * clamped;
            double pz = a.z + uz * clamped;
            double distance = (x - px) * (x - px) + (z - pz) * (z - pz);
            if (distance < best - EPSILON) {
                best = distance;
                double across = (x - px) * -uz + (z - pz) * ux;
                side = Math.abs(across) < EPSILON && distance > EPSILON ? Math.sqrt(distance) : Math.copySign(Math.sqrt(distance), across);
            }
        }
        return side;
    }
}
