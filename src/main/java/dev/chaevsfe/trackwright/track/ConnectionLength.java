package dev.chaevsfe.trackwright.track;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CServer;
import dev.chaevsfe.trackwright.config.Sides;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class ConnectionLength {
    public static final int CURVE_REACH_MARGIN = 16;

    private static final ThreadLocal<Integer> CURVE_REACH = new ThreadLocal<>();

    private ConnectionLength() {
    }

    public static int limit(int createLength, @Nullable TrackwrightSettings settings) {
        return settings == null ? createLength : Math.max(createLength, settings.maxConnectionLength());
    }

    public static Object forPlacement(Object value, Level level) {
        if (!(value instanceof Integer createLength)) {
            return value;
        }
        return limit(createLength, Sides.forLevel(level));
    }

    public static Object forTargeting(Object value, UseOnContext context) {
        if (!(value instanceof Integer createLength)) {
            return value;
        }
        Level level = context.getLevel();
        int limit = limit(createLength, Sides.forLevel(level));
        ItemStack stack = context.getItemInHand();
        BlockPos selected = stack.get(AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS);
        BezierTrackPointLocation bezier = stack.get(AllDataComponents.TRACK_TARGETING_ITEM_BEZIER);
        if (selected != null && bezier != null) {
            limit = Math.max(limit, curveLength(level, selected, bezier.curveTarget()));
        }
        return limit;
    }

    public static int curveLength(Level level, BlockPos pos, BlockPos target) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof TrackBlockEntity track)) {
            return 0;
        }
        BezierConnection connection = track.getConnections().get(target);
        if (connection == null) {
            return 0;
        }
        double length = connection.getLength();
        return Double.isFinite(length) ? Mth.ceil(length) : 0;
    }

    public static @Nullable Integer enterCurveReach(int reach) {
        Integer previous = CURVE_REACH.get();
        CURVE_REACH.set(reach);
        return previous;
    }

    public static void exitCurveReach(@Nullable Integer previous) {
        if (previous == null) {
            CURVE_REACH.remove();
        } else {
            CURVE_REACH.set(previous);
        }
    }

    public static Object duringCurveReach(Object configValue, Object value) {
        Integer reach = CURVE_REACH.get();
        if (reach == null || !(value instanceof Integer createLength) || reach <= createLength) {
            return value;
        }
        CServer server = AllConfigs.server();
        if (server == null || configValue != server.trains.maxTrackPlacementLength) {
            return value;
        }
        return reach;
    }
}
