package dev.chaevsfe.trackwright.client.guide;

import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.zurrtum.create.infrastructure.component.ConnectingFrom;
import com.zurrtum.create.infrastructure.config.AllConfigs;
import com.zurrtum.create.infrastructure.config.CServer;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import dev.chaevsfe.trackwright.track.BoreAccess;
import dev.chaevsfe.trackwright.track.BorePlan;
import dev.chaevsfe.trackwright.track.ConnectionLength;
import dev.chaevsfe.trackwright.track.Messages;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import dev.chaevsfe.trackwright.track.PlanGeometry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
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

    public static List<Component> waiting(ConnectingFrom from, int walkCloser) {
        List<Component> lines = new ArrayList<>();
        lines.add(title());
        lines.add(line("trackwright.guide.start", position(from.pos())));
        if (walkCloser > 0) {
            lines.add(outOfReach(walkCloser));
        } else {
            lines.add(Component.translatable("trackwright.guide.aim").withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    public static List<Component> undoHint() {
        return List.of(
            title(),
            Component.translatable("trackwright.guide.undo", Component.keybind(UndoKey.KEY_NAME).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY)
        );
    }

    public static Component outOfReach(int walkCloser) {
        String blocks = Integer.toString(walkCloser);
        return Component.translatable(
            walkCloser == 1 ? "trackwright.guide.out_of_reach_one" : "trackwright.guide.out_of_reach",
            Component.literal(blocks).withStyle(ChatFormatting.WHITE)
        ).withStyle(ChatFormatting.YELLOW);
    }

    public static boolean beforeGeometry(String message) {
        return BEFORE_GEOMETRY.contains(message);
    }

    public static boolean geometryFinal(PlacementInfo info) {
        return info.valid || info.message != null && COST_KNOWN.contains(info.message);
    }

    public static List<Component> planned(ConnectingFrom from, BlockPos target, PlacementInfo info, PlanExtras extras, ItemStack tool) {
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
            lines.add(counted("trackwright.guide.length", "trackwright.guide.length_one", number(horizontal)));
            Integer limit = connectionLimit();
            if (limit != null) {
                lines.add(line("trackwright.guide.reach", number(Math.sqrt(start.distSqr(target))), Integer.toString(limit)));
            }
            if (info.valid || info.message != null && !BEFORE_GEOMETRY.contains(info.message)) {
                addGeometry(lines, info, dx, dz);
            }
            if (info.valid) {
                PlanGeometry.Climb climb = PlanGeometry.climb(info.curve, dx, dy, dz);
                boolean climbs = Math.abs(climb.rise()) >= 0.05;
                if (climbs) {
                    addClimb(lines, climb);
                }
                addShape(lines, info, extras, climbs && inLine(info, dx, dz));
            }
            if (info.valid || info.message != null && COST_KNOWN.contains(info.message)) {
                lines.add(line("trackwright.guide.tracks", Integer.toString(info.requiredTracks)));
                if (info.requiredPavement > 0) {
                    lines.add(line("trackwright.guide.paving", Integer.toString(info.requiredPavement)));
                }
            }
            PlacementInfoExtension extension = (PlacementInfoExtension) info;
            if (info.valid && extension.trackwright$bridged()) {
                int through = extension.trackwright$bridgedCells().size();
                lines.add(Component.translatable(
                    through == 1 ? "trackwright.guide.through_blocks_one" : "trackwright.guide.through_blocks",
                    Component.literal(Integer.toString(through)).withStyle(ChatFormatting.WHITE)
                ).withStyle(ChatFormatting.AQUA));
                lines.add(Component.translatable("trackwright.guide.bridge_limits").withStyle(ChatFormatting.GRAY));
            }
            addBoreAccess(lines, extension);
            BorePlan bore = extension.trackwright$bore();
            if (bore != null) {
                addBore(lines, bore, tool);
            }
            if (info.valid && extension.trackwright$converts()) {
                lines.add(Component.translatable("trackwright.guide.convert").withStyle(ChatFormatting.AQUA));
            }
            if (extension.trackwright$guessed()) {
                lines.add(Component.translatable("trackwright.guide.out_of_view").withStyle(ChatFormatting.YELLOW));
            }
            HeightColumn column = extras.column();
            if (column != null && !geometryFinal(info)) {
                lines.add(heights(column, target));
            }
        }
        Component result = result(info);
        if (result != null) {
            lines.add(result);
        }
        return lines;
    }

    private static void addGeometry(List<Component> lines, PlacementInfo info, int dx, int dz) {
        double change = PlanGeometry.headingChange(info.axis1, info.axis2);
        if (!Double.isNaN(change)) {
            if (Math.abs(change) < 0.5) {
                double sideways = PlanGeometry.sideways(info.axis1, dx, dz);
                if (Double.isNaN(sideways) || Math.abs(sideways) < 0.05) {
                    lines.add(Component.translatable("trackwright.guide.turn_none").withStyle(ChatFormatting.GRAY));
                } else {
                    lines.add(counted("trackwright.guide.shift", "trackwright.guide.shift_one", number(Math.abs(sideways))));
                }
            } else {
                lines.add(line(change > 0 ? "trackwright.guide.turn_right" : "trackwright.guide.turn_left", number(Math.abs(change))));
            }
        }
        BezierConnection curve = info.curve;
        if (curve != null) {
            double radius = PlanGeometry.tightestRadius(curve);
            if (radius < STRAIGHT_RADIUS) {
                lines.add(counted("trackwright.guide.radius", "trackwright.guide.radius_one", number(radius)));
            }
        }
    }

    private static boolean inLine(PlacementInfo info, int dx, int dz) {
        double change = PlanGeometry.headingChange(info.axis1, info.axis2);
        if (!Double.isNaN(change) && Math.abs(change) >= 0.5) {
            return false;
        }
        double sideways = PlanGeometry.sideways(info.axis1, dx, dz);
        return Double.isNaN(sideways) || Math.abs(sideways) < 0.05;
    }

    private static void addClimb(List<Component> lines, PlanGeometry.Climb climb) {
        String rise = number(Math.abs(climb.rise()));
        boolean one = "1".equals(rise);
        String key = climb.rise() > 0
            ? one ? "trackwright.guide.climb_one" : "trackwright.guide.climb"
            : one ? "trackwright.guide.descend_one" : "trackwright.guide.descend";
        lines.add(line(key, rise, number(climb.run()), Math.round(climb.steepest() * 100) + "%"));
    }

    private static void addShape(List<Component> lines, PlacementInfo info, PlanExtras extras, boolean climbOnly) {
        if (info.curve == null) {
            return;
        }
        boolean depends = extras.shapeDependsOnWidest(info);
        if (extras.maximised()) {
            if (extras.locked()) {
                lines.add(Component.translatable(climbOnly ? "trackwright.guide.climb_spread_locked" : "trackwright.guide.widest_turn_locked")
                    .withStyle(ChatFormatting.GRAY));
            } else if (depends) {
                lines.add(Component.translatable(climbOnly ? "trackwright.guide.climb_spread" : "trackwright.guide.widest_turn")
                    .withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (!depends) {
            return;
        }
        if (climbOnly) {
            lines.add(hold("trackwright.guide.hold_spread", "trackwright.guide.hold_spread_lock"));
            return;
        }
        lines.add(line("trackwright.guide.tightest_turn", Integer.toString(info.end1Extent), Integer.toString(info.end2Extent)));
        lines.add(hold("trackwright.guide.hold_widest", "trackwright.guide.hold_widest_lock"));
    }

    private static void addBoreAccess(List<Component> lines, PlacementInfoExtension extension) {
        BoreAccess access = extension.trackwright$boreAccess();
        String key = switch (access) {
            case NO_TOOL -> "trackwright.guide.bore_hint";
            case OFF -> "trackwright.guide.bore_off";
            case CREATIVE_ONLY -> "trackwright.guide.bore_creative_only";
            case NONE, ALLOWED -> null;
        };
        if (key != null) {
            lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void addBore(List<Component> lines, BorePlan bore, ItemStack tool) {
        int blocks = bore.cells().size();
        boolean one = blocks == 1;
        Component count = white(Integer.toString(blocks));
        Component toolName = tool.getHoverName().copy().withStyle(ChatFormatting.WHITE);
        if (bore.creative()) {
            lines.add(Component.translatable(one ? "trackwright.guide.bore_creative_one" : "trackwright.guide.bore_creative", count)
                .withStyle(ChatFormatting.GOLD));
        } else if (bore.usesLeft() < 0) {
            lines.add(Component.translatable(one ? "trackwright.guide.bore_free_one" : "trackwright.guide.bore_free", count, toolName)
                .withStyle(ChatFormatting.GOLD));
        } else {
            lines.add(Component.translatable(one ? "trackwright.guide.bore_one" : "trackwright.guide.bore", count, toolName,
                white(Integer.toString(bore.wear())), white(Integer.toString(bore.usesLeft()))).withStyle(ChatFormatting.GOLD));
        }
        lines.add(line(bore.creative() ? "trackwright.guide.bore_tunnel_creative" : "trackwright.guide.bore_tunnel",
            Integer.toString(bore.headroom() + 1)));
        BlockMiner.Problem problem = bore.problem();
        if (problem != null && problem.refusal() != BlockMiner.Refusal.NOT_LOADED) {
            lines.add(Component.translatable("trackwright.guide.bore_problem", problem.state().getBlock().getName().withStyle(ChatFormatting.WHITE),
                white(position(problem.pos()))).withStyle(ChatFormatting.RED));
        }
    }

    private static Component white(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    private static Component heights(HeightColumn column, BlockPos target) {
        Integer nearest = column.nearest();
        if (nearest == null) {
            return line("trackwright.guide.no_height", Integer.toString(target.getY() - HeightColumn.SPAN), Integer.toString(target.getY() + HeightColumn.SPAN));
        }
        int change = nearest - target.getY();
        String blocks = Integer.toString(Math.abs(change));
        String key = change > 0
            ? change == 1 ? "trackwright.guide.connects_higher_one" : "trackwright.guide.connects_higher"
            : change == -1 ? "trackwright.guide.connects_lower_one" : "trackwright.guide.connects_lower";
        return line(key, blocks, Integer.toString(nearest));
    }

    private static Component hold(String key, String lockKey) {
        Component sprint = Component.keybind("key.sprint").withStyle(ChatFormatting.WHITE);
        if (WidestTurnLock.bound()) {
            return Component.translatable(lockKey, sprint, Component.keybind(WidestTurnLock.KEY_NAME).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable(key, sprint).withStyle(ChatFormatting.GRAY);
    }

    private static @Nullable Component result(PlacementInfo info) {
        if (info.valid) {
            return Component.translatable("create.track.valid_connection").withStyle(ChatFormatting.GREEN);
        }
        if (info.message == null) {
            return null;
        }
        if (COST_KNOWN.contains(info.message) && ((PlacementInfoExtension) info).trackwright$guessed()) {
            return Component.translatable("trackwright.guide.server_counts").withStyle(ChatFormatting.GRAY);
        }
        String key = "create." + info.message;
        String fallback = Messages.fallback(key);
        MutableComponent text = fallback != null ? Component.translatableWithFallback(key, fallback) : Component.translatable(key);
        return text.withStyle("track.second_point".equals(info.message) ? ChatFormatting.WHITE : ChatFormatting.RED);
    }

    public static @Nullable Integer connectionLimit() {
        CServer server = AllConfigs.server();
        if (server == null) {
            return null;
        }
        return ConnectionLength.limit(server.trains.maxTrackPlacementLength.get(), RemoteSettings.get());
    }

    private static Component title() {
        return Component.translatable("trackwright.guide.title").withStyle(ChatFormatting.GOLD);
    }

    private static Component counted(String key, String oneKey, String value) {
        return line("1".equals(value) ? oneKey : key, value);
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
