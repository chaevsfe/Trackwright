package dev.chaevsfe.trackwright.client.guide;

import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.zurrtum.create.infrastructure.component.ConnectingFrom;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import com.zurrtum.create.infrastructure.config.CServer;
import dev.chaevsfe.trackwright.track.Messages;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import dev.chaevsfe.trackwright.track.PlanGeometry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class GuideLines {
    private static final Set<String> BEFORE_GEOMETRY = Set.of(
        "track.second_point",
        "track.too_far",
        "track.original_missing",
        "track.turn_start"
    );
    private static final Set<String> COST_KNOWN = Set.of("track.not_enough_tracks", "track.not_enough_pavement");
    private static final double STRAIGHT_RADIUS = 1000;

    private GuideLines() {
    }

    public static List<Component> waiting(ConnectingFrom from) {
        List<Component> lines = new ArrayList<>();
        lines.add(title());
        lines.add(line("trackwright.guide.start", position(from.pos())));
        lines.add(Component.translatable("trackwright.guide.aim").withStyle(ChatFormatting.GRAY));
        return lines;
    }

    public static List<Component> planned(ConnectingFrom from, BlockPos target, PlacementInfo info) {
        List<Component> lines = new ArrayList<>();
        lines.add(title());
        BlockPos start = from.pos();
        lines.add(line("trackwright.guide.start", position(start)));
        if (!start.equals(target)) {
            int dx = target.getX() - start.getX();
            int dy = target.getY() - start.getY();
            int dz = target.getZ() - start.getZ();
            lines.add(line("trackwright.guide.offset", signed(dx), signed(dy), signed(dz)));
            double horizontal = Math.sqrt((double) dx * dx + (double) dz * dz);
            lines.add(line("trackwright.guide.length", number(horizontal)));
            Integer limit = connectionLimit();
            if (limit != null) {
                lines.add(line("trackwright.guide.reach", number(Math.sqrt(start.distSqr(target))), Integer.toString(limit)));
            }
            if (info.valid || info.message != null && !BEFORE_GEOMETRY.contains(info.message)) {
                addGeometry(lines, info, dx, dy, dz, horizontal);
            }
            if (info.valid || info.message != null && COST_KNOWN.contains(info.message)) {
                lines.add(line("trackwright.guide.tracks", Integer.toString(info.requiredTracks)));
                if (info.requiredPavement > 0) {
                    lines.add(line("trackwright.guide.paving", Integer.toString(info.requiredPavement)));
                }
            }
            PlacementInfoExtension extension = (PlacementInfoExtension) info;
            if (info.valid && extension.trackwright$bridged()) {
                lines.add(Component.translatable(
                    "trackwright.guide.through_blocks",
                    Component.literal(Integer.toString(extension.trackwright$bridgedCells().size())).withStyle(ChatFormatting.WHITE)
                ).withStyle(ChatFormatting.AQUA));
            }
        }
        Component result = result(info);
        if (result != null) {
            lines.add(result);
        }
        return lines;
    }

    private static void addGeometry(List<Component> lines, PlacementInfo info, int dx, int dy, int dz, double horizontal) {
        double change = PlanGeometry.headingChange(info.axis1, info.axis2);
        if (!Double.isNaN(change)) {
            if (Math.abs(change) < 0.5) {
                double sideways = PlanGeometry.sideways(info.axis1, dx, dz);
                if (Double.isNaN(sideways) || Math.abs(sideways) < 0.05) {
                    lines.add(Component.translatable("trackwright.guide.turn_none").withStyle(ChatFormatting.GRAY));
                } else {
                    lines.add(line("trackwright.guide.shift", number(Math.abs(sideways))));
                }
            } else {
                lines.add(line(change > 0 ? "trackwright.guide.turn_right" : "trackwright.guide.turn_left", number(Math.abs(change))));
            }
        }
        BezierConnection curve = info.curve;
        if (curve != null) {
            double radius = PlanGeometry.tightestRadius(curve);
            if (radius < STRAIGHT_RADIUS) {
                lines.add(line("trackwright.guide.radius", number(radius)));
            }
        }
        if (horizontal > 0) {
            lines.add(line("trackwright.guide.grade", String.format(Locale.ROOT, "%+.1f%%", dy / horizontal * 100)));
        }
    }

    private static @Nullable Component result(PlacementInfo info) {
        if (info.valid) {
            return Component.translatable("create.track.valid_connection").withStyle(ChatFormatting.GREEN);
        }
        if (info.message == null) {
            return null;
        }
        String key = "create." + info.message;
        String fallback = Messages.fallback(key);
        MutableComponent text = fallback != null ? Component.translatableWithFallback(key, fallback) : Component.translatable(key);
        return text.withStyle("track.second_point".equals(info.message) ? ChatFormatting.WHITE : ChatFormatting.RED);
    }

    private static @Nullable Integer connectionLimit() {
        CServer server = AllConfigs.server();
        if (server == null) {
            return null;
        }
        return server.trains.maxTrackPlacementLength.get();
    }

    private static Component title() {
        return Component.translatable("trackwright.guide.title").withStyle(ChatFormatting.GOLD);
    }

    private static Component line(String key, String... values) {
        Object[] arguments = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            arguments[i] = Component.literal(values[i]).withStyle(ChatFormatting.WHITE);
        }
        return Component.translatable(key, arguments).withStyle(ChatFormatting.GRAY);
    }

    private static String position(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private static String number(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.05) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
