package dev.chaevsfe.trackwright.track;

import dev.chaevsfe.trackwright.config.BoreProfile;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BoreBand {
    private static final double END_REACH = Math.sqrt(0.5);
    private static final double EPSILON = 1.0E-6;
    private static final int BUCKET = 2;
    private static final IntArrayList NONE = new IntArrayList();

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
        LongOpenHashSet columns = new LongOpenHashSet();
        for (BlockPos cell : line.cells()) {
            columns.add(column(cell.getX(), cell.getZ()));
        }
        Segments segments = new Segments(points, reach + 1);
        Owners owners = new Owners(line.cells(), reach * 1.5 + 1);
        LongOpenHashSet seen = new LongOpenHashSet();
        for (BlockPos seed : line.cells()) {
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    int x = seed.getX() + dx;
                    int z = seed.getZ() + dz;
                    long key = column(x, z);
                    if (columns.contains(key) || !seen.add(key)) {
                        continue;
                    }
                    double side = segments.side(x + 0.5, z + 0.5);
                    if (Double.isNaN(side) || side <= leftEdge + EPSILON || side >= rightEdge - EPSILON) {
                        continue;
                    }
                    BlockPos owner = owners.nearest(x + 0.5, z + 0.5);
                    found.get(owner).add(new BlockPos(x, owner.getY(), z));
                }
            }
        }
        return found;
    }

    private static long column(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static long bucket(double x, double z) {
        return column(Math.floorDiv((int) Math.floor(x), BUCKET), Math.floorDiv((int) Math.floor(z), BUCKET));
    }

    private static void spread(Long2ObjectOpenHashMap<IntArrayList> buckets, int index, double minX, double minZ, double maxX, double maxZ) {
        int fromX = Math.floorDiv((int) Math.floor(minX), BUCKET);
        int toX = Math.floorDiv((int) Math.floor(maxX), BUCKET);
        int fromZ = Math.floorDiv((int) Math.floor(minZ), BUCKET);
        int toZ = Math.floorDiv((int) Math.floor(maxZ), BUCKET);
        for (int bx = fromX; bx <= toX; bx++) {
            for (int bz = fromZ; bz <= toZ; bz++) {
                buckets.computeIfAbsent(column(bx, bz), key -> new IntArrayList()).add(index);
            }
        }
    }

    private static final class Owners {
        private final List<BlockPos> cells;
        private final Long2ObjectOpenHashMap<IntArrayList> buckets = new Long2ObjectOpenHashMap<>();

        private Owners(List<BlockPos> cells, double margin) {
            this.cells = cells;
            for (int i = 0; i < cells.size(); i++) {
                BlockPos cell = cells.get(i);
                double x = cell.getX() + 0.5;
                double z = cell.getZ() + 0.5;
                spread(buckets, i, x - margin, z - margin, x + margin, z + margin);
            }
        }

        private BlockPos nearest(double x, double z) {
            BlockPos best = cells.getFirst();
            double bestDistance = Double.MAX_VALUE;
            IntArrayList indices = buckets.getOrDefault(bucket(x, z), NONE);
            for (int n = 0; n < indices.size(); n++) {
                BlockPos cell = cells.get(indices.getInt(n));
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
    }

    private static final class Segments {
        private static final int STRIDE = 8;
        private final double[] segments;
        private final Long2ObjectOpenHashMap<IntArrayList> buckets = new Long2ObjectOpenHashMap<>();

        private Segments(List<Vec3> points, double margin) {
            int last = points.size() - 2;
            segments = new double[Math.max(0, last + 1) * STRIDE];
            int count = 0;
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
                double from = i == 0 ? -END_REACH : 0;
                double to = i == last ? length + END_REACH : length;
                double startX = a.x + ux * from;
                double startZ = a.z + uz * from;
                double endX = a.x + ux * to;
                double endZ = a.z + uz * to;
                spread(buckets, count, Math.min(startX, endX) - margin, Math.min(startZ, endZ) - margin,
                    Math.max(startX, endX) + margin, Math.max(startZ, endZ) + margin);
                int at = count * STRIDE;
                segments[at] = a.x;
                segments[at + 1] = a.z;
                segments[at + 2] = ux;
                segments[at + 3] = uz;
                segments[at + 4] = from;
                segments[at + 5] = to;
                segments[at + 6] = i == 0 ? 1 : 0;
                segments[at + 7] = i == last ? 1 : 0;
                count++;
            }
        }

        private double side(double x, double z) {
            double best = Double.MAX_VALUE;
            double side = Double.NaN;
            IntArrayList indices = buckets.getOrDefault(bucket(x, z), NONE);
            for (int n = 0; n < indices.size(); n++) {
                int at = indices.getInt(n) * STRIDE;
                double ax = segments[at];
                double az = segments[at + 1];
                double ux = segments[at + 2];
                double uz = segments[at + 3];
                double from = segments[at + 4];
                double to = segments[at + 5];
                boolean first = segments[at + 6] != 0;
                boolean last = segments[at + 7] != 0;
                double along = (x - ax) * ux + (z - az) * uz;
                if (along < from - EPSILON || along > to + EPSILON) {
                    if (first && along < from || last && along > to) {
                        continue;
                    }
                }
                double clamped = Math.max(from, Math.min(to, along));
                double px = ax + ux * clamped;
                double pz = az + uz * clamped;
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
}
