package dev.chaevsfe.trackwright.client.guide;

import com.zurrtum.create.client.AllSpecialTextures;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import com.zurrtum.create.client.content.trains.track.TrackBlockOutline;
import com.zurrtum.create.client.content.trains.track.TrackBlockOutline.BezierPointSelection;
import com.zurrtum.create.content.trains.track.BezierConnection;
import com.zurrtum.create.content.trains.track.ITrackBlock;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import dev.chaevsfe.trackwright.client.input.CreateClickFirst;
import dev.chaevsfe.trackwright.config.BoreMode;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.SettingMessages;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import dev.chaevsfe.trackwright.mining.BlockMiner;
import dev.chaevsfe.trackwright.network.ClearBedPayload;
import dev.chaevsfe.trackwright.track.BorePlan;
import dev.chaevsfe.trackwright.track.BoreProfiles;
import dev.chaevsfe.trackwright.track.ClearBed;
import dev.chaevsfe.trackwright.track.ClearBedPlan;
import dev.chaevsfe.trackwright.track.ClearMessages;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ClearBedPreview {
    private static final int REPLAN_TICKS = 10;
    private static final String TRACK_OUTLINE = "trackwright_clear_track";
    private static final String BAND_OUTLINE = "trackwright_clear_band";
    private static final String PROBLEM_OUTLINE = "trackwright_clear_problem";
    private static final int TRACK_COLOR = 0xF0A030;
    private static final int BAND_COLOR = 0xF6D28C;
    private static final int PROBLEM_COLOR = 0xEA5C2B;

    private static @Nullable Target aimed;
    private static @Nullable ClearBedPlan plan;
    private static @Nullable PlanKey planKey;
    private static int age;
    private static List<Component> lines = List.of();
    private static boolean sentWhileHeld;

    public record Target(BlockPos track, BlockPos target, BezierConnection connection, Vec3 aim, Vec3 tangent) {
    }

    private record PlanKey(BlockPos track, BlockPos target, BezierConnection connection, BoreProfile profile, Item tool, int damage, boolean creative) {
    }

    private ClearBedPreview() {
    }

    public static void reset() {
        aimed = null;
        plan = null;
        planKey = null;
        age = 0;
        lines = List.of();
        sentWhileHeld = false;
    }

    public static boolean beforeUseItem(Minecraft mc, LocalPlayer player, @Nullable BezierPointSelection curve) {
        if (!player.isShiftKeyDown() || !BlockMiner.isTool(player.getMainHandItem()) || ClearBed.placesOffHand(player)) {
            return false;
        }
        Target target = target(mc, player, curve);
        TrackwrightSettings settings = RemoteSettings.get();
        if (target == null || settings == null || !ClientPlayNetworking.canSend(ClearBedPayload.TYPE) || !settings.clearTrackBed().allows(player)) {
            return false;
        }
        if (sentWhileHeld) {
            return true;
        }
        BoreProfile profile = profile(player, target, settings);
        ClearBedPlan current = ClearBedPlan.of(player.level(), target.track(), target.connection(), player.getMainHandItem(), player.isCreative(), profile,
            settings);
        Long hash = current.guessed() ? null : current.hash();
        ClientPlayNetworking.send(ClearBedPayload.of(target.track(), target.target(), target.aim(), BoreShaping.shaped(), hash, current.count()));
        player.swing(InteractionHand.MAIN_HAND);
        sentWhileHeld = true;
        plan = null;
        return true;
    }

    public static void tick(Minecraft mc) {
        if (!mc.options.keyUse.isDown()) {
            sentWhileHeld = false;
        }
        aimed = null;
        lines = List.of();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isSpectator() || !BlockMiner.isTool(player.getMainHandItem())) {
            plan = null;
            return;
        }
        Target target = target(mc, player, TrackBlockOutline.result);
        if (target == null || CreateClickFirst.contraptionHit(mc)) {
            plan = null;
            return;
        }
        aimed = target;
        if (!player.isShiftKeyDown() && !BoreShaping.held() || ClearBed.placesOffHand(player)) {
            plan = null;
            return;
        }
        TrackwrightSettings settings = RemoteSettings.get();
        if (settings == null || !ClientPlayNetworking.canSend(ClearBedPayload.TYPE)) {
            plan = null;
            if (player.isShiftKeyDown()) {
                lines = List.of(SettingMessages.component(SettingMessages.CLEAR_NEEDS_SERVER).withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (!settings.clearTrackBed().allows(player)) {
            plan = null;
            String key = settings.clearTrackBed() == BoreMode.OFF ? SettingMessages.CLEAR_OFF : SettingMessages.CLEAR_CREATIVE_ONLY;
            lines = List.of(title(), SettingMessages.component(key).withStyle(ChatFormatting.GRAY));
            return;
        }
        ItemStack tool = player.getMainHandItem();
        BoreProfile profile = profile(player, target, settings);
        PlanKey key = new PlanKey(target.track(), target.target(), target.connection(), profile, tool.getItem(), tool.getDamageValue(), player.isCreative());
        ClearBedPlan current = plan;
        if (current == null || !key.equals(planKey) || ++age >= REPLAN_TICKS) {
            current = ClearBedPlan.of(mc.level, target.track(), target.connection(), tool, player.isCreative(), profile, settings);
            plan = current;
            planKey = key;
            age = 0;
        }
        show(current.bore());
        lines = lines(player, current, profile, tool);
    }

    public static List<Component> lines() {
        return lines;
    }

    public static @Nullable Target aimed() {
        return aimed;
    }

    public static boolean shaping(LocalPlayer player) {
        return aimed != null && BlockMiner.isTool(player.getMainHandItem());
    }

    public static @Nullable BoreProfile profile(LocalPlayer player) {
        Target target = aimed;
        TrackwrightSettings settings = RemoteSettings.get();
        return target == null || settings == null ? null : profile(player, target, settings);
    }

    private static BoreProfile profile(LocalPlayer player, Target target, TrackwrightSettings settings) {
        return BoreProfiles.resolve(player.level(), player, target.connection().getMaterial(), settings);
    }

    private static @Nullable Target target(Minecraft mc, LocalPlayer player, @Nullable BezierPointSelection curve) {
        Level level = mc.level;
        if (level == null) {
            return null;
        }
        if (curve != null) {
            TrackBlockEntity track = curve.blockEntity();
            BlockPos other = curve.loc().curveTarget();
            BezierConnection connection = track.getConnections().get(other);
            if (connection != null && !track.isRemoved()) {
                return new Target(track.getBlockPos().immutable(), other.immutable(), connection, curve.vec(), curve.direction());
            }
        }
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof ITrackBlock) || !(level.getBlockEntity(pos) instanceof TrackBlockEntity track)
            || track.getConnections().isEmpty()) {
            return null;
        }
        BezierConnection connection = ClearBedPlan.pick(track, player.getLookAngle());
        if (connection == null) {
            return null;
        }
        return new Target(pos.immutable(), connection.getKey().immutable(), connection, hit.getLocation(), connection.axes.getFirst());
    }

    private static void show(BorePlan bore) {
        BlockMiner.Problem problem = bore.problem();
        if (problem != null && problem.refusal() != BlockMiner.Refusal.NOT_LOADED) {
            Outliner.getInstance().showAABB(PROBLEM_OUTLINE, new AABB(problem.pos()).inflate(1 / 64.0))
                .colored(PROBLEM_COLOR)
                .disableLineNormals()
                .lineWidth(1 / 16f)
                .withFaceTexture(AllSpecialTextures.CHECKERED);
        }
        List<BlockPos> path = bore.pathCells();
        if (!path.isEmpty()) {
            Outliner.getInstance().showCluster(TRACK_OUTLINE, path)
                .colored(TRACK_COLOR)
                .disableLineNormals()
                .lineWidth(1 / 32f)
                .withFaceTexture(AllSpecialTextures.CHECKERED);
        }
        if (!bore.bandCells().isEmpty()) {
            Outliner.getInstance().showCluster(BAND_OUTLINE, bore.bandCells())
                .colored(BAND_COLOR)
                .disableLineNormals()
                .lineWidth(1 / 64f)
                .withFaceTexture(AllSpecialTextures.THIN_CHECKERED);
        }
    }

    private static List<Component> lines(LocalPlayer player, ClearBedPlan current, BoreProfile profile, ItemStack tool) {
        List<Component> found = new ArrayList<>();
        found.add(title());
        found.add(GuideLines.profileLine(profile));
        BorePlan bore = current.bore();
        if (bore.cells().isEmpty() && bore.problem() == null) {
            found.add(ClearMessages.component(ClearMessages.NOTHING).withStyle(ChatFormatting.GRAY));
            addKept(found, bore);
            found.add(shapeHint());
            return found;
        }
        boolean refused = bore.problem() != null;
        GuideLines.addBore(found, bore, tool, refused);
        addKept(found, bore);
        if (current.water() > 0) {
            found.add(Component.translatable("trackwright.guide.clear_water").withStyle(ChatFormatting.YELLOW));
        }
        found.add(Component.translatable("trackwright.guide.clear_no_undo").withStyle(ChatFormatting.GRAY));
        if (current.guessed()) {
            found.add(Component.translatable("trackwright.guide.out_of_view").withStyle(ChatFormatting.YELLOW));
        }
        if (!refused) {
            found.add(Component.translatable("trackwright.guide.clear_hint").withStyle(ChatFormatting.AQUA));
        }
        found.add(shapeHint());
        return found;
    }

    private static void addKept(List<Component> found, BorePlan bore) {
        int kept = bore.keptCells().size();
        if (kept > 0) {
            found.add(Component.translatable(kept == 1 ? "trackwright.guide.clear_kept_one" : "trackwright.guide.clear_kept",
                Component.literal(Integer.toString(kept)).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component shapeHint() {
        return Component.translatable("trackwright.shape.hint", Component.keybind(BoreShaping.KEY_NAME).withStyle(ChatFormatting.WHITE))
            .withStyle(ChatFormatting.GRAY);
    }

    private static Component title() {
        return Component.translatable("trackwright.guide.clear_title").withStyle(ChatFormatting.GOLD);
    }
}
