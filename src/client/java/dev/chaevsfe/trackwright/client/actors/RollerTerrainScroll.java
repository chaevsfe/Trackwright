package dev.chaevsfe.trackwright.client.actors;

import com.zurrtum.create.client.Create;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import com.zurrtum.create.client.content.contraptions.actors.roller.RollerValueBox;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.zurrtum.create.client.foundation.gui.AllIcons;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.BehaviourType;
import com.zurrtum.create.catnip.math.VecHelper;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.content.contraptions.actors.roller.RollerBlock;
import dev.chaevsfe.trackwright.actors.RollerTerrain;
import dev.chaevsfe.trackwright.actors.RollerTerrainBehaviour;
import dev.chaevsfe.trackwright.client.mixin.ScrollValueRendererAccessor;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RollerTerrainScroll extends ScrollOptionBehaviour<RollerTerrain> {
    public static final BehaviourType<RollerTerrainScroll> TYPE = new BehaviourType<>("trackwright_roller_terrain_box");

    public RollerTerrainScroll(SmartBlockEntity blockEntity) {
        super(Icon.class, terrain -> Icon.values()[terrain.ordinal()], Component.translatable("trackwright.roller.terrain"), blockEntity, new Slot());
        behaviour = blockEntity.getBehaviour(RollerTerrainBehaviour.TYPE);
        onlyActiveWhen(this::available);
    }

    @Override
    public void initialize() {
        if (behaviour == null) {
            behaviour = blockEntity.getBehaviour(RollerTerrainBehaviour.TYPE);
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    private boolean available() {
        TrackwrightSettings settings = RemoteSettings.get();
        return behaviour != null && settings != null && settings.rollerReshape();
    }

    public static void tick(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || minecraft.level == null || minecraft.player == null) {
            return;
        }
        BlockEntity found = minecraft.level.getBlockEntity(hit.getBlockPos());
        if (!(found instanceof SmartBlockEntity blockEntity)) {
            return;
        }
        RollerTerrainScroll box = blockEntity.getBehaviour(TYPE);
        if (box == null) {
            return;
        }
        if (!box.isActive()) {
            Outliner.getInstance().remove(box);
            return;
        }
        ItemStack held = minecraft.player.getMainHandItem();
        boolean highlight = box.testHit(hit.getLocation()) && !box.bypassesInput(held);
        ScrollValueRendererAccessor.trackwright$addBox(hit.getBlockPos(), hit.getDirection(), box, highlight);
        if (highlight) {
            List<MutableComponent> tip = new ArrayList<>();
            tip.add(box.label.copy());
            tip.add(Component.translatable("create.gui.value_settings.hold_to_edit"));
            Create.VALUE_SETTINGS_HANDLER.showHoverTip(minecraft, tip);
        }
    }

    public enum Icon implements INamedIconOptions {
        EVERYTHING(AllIcons.I_ROLLER_PAVE, "trackwright.roller.terrain.everything"),
        FILTER_ONLY(AllIcons.I_WHITELIST, "trackwright.roller.terrain.reshape");

        private final AllIcons icon;
        private final String translationKey;

        Icon(AllIcons icon, String translationKey) {
            this.icon = icon;
            this.translationKey = translationKey;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }

    public static final class Slot extends RollerValueBox {
        public Slot() {
            super(0);
        }

        @Override
        public Vec3 getLocalOffset(BlockState state) {
            Direction facing = state.getValue(RollerBlock.FACING);
            return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 15.5, 5), AngleHelper.horizontalAngle(facing) + 180, Direction.Axis.Y);
        }
    }
}
