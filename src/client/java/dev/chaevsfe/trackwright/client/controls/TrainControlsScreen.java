package dev.chaevsfe.trackwright.client.controls;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import dev.chaevsfe.trackwright.actors.TrainControlMessages;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.SettingMessages;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TrainControlsScreen extends Screen {
    public static final String TITLE = "trackwright.train_controls.title";
    public static final String LIST_TITLE = "trackwright.train_controls.list_title";
    public static final String TARGET = "trackwright.train_controls.target";
    public static final String SLOT = "trackwright.train_controls.slot";
    public static final String SLOT_EMPTY = "trackwright.train_controls.slot_empty";
    public static final String SLOT_THIS = "trackwright.train_controls.slot_this";
    public static final String HELD = "trackwright.train_controls.held";
    public static final String HINT = "trackwright.train_controls.hint";
    public static final String ROW = "trackwright.train_controls.row";
    public static final String NOT_LOADED_ROW = "trackwright.train_controls.not_loaded_row";
    public static final String SWITCH = "trackwright.train_controls.switch";
    public static final String MORE = "trackwright.train_controls.more";
    public static final String SHOW_ALL = "trackwright.train_controls.show_all";
    public static final String NONE = "trackwright.train_controls.none";
    public static final String STATE_ON = "create.contraption.controls.actor_toggle.on";
    public static final String STATE_OFF = "create.contraption.controls.actor_toggle.off";

    private static final int MAX_ROWS = 8;
    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TEXT_WIDTH = 200;
    private static final int STATE_WIDTH = 30;
    private static final int SWITCH_WIDTH = 50;
    private static final int SLOT_WIDTH = 20;
    private static final int GAP = 4;
    private static final int LIST_WIDTH = TEXT_WIDTH + STATE_WIDTH + SWITCH_WIDTH + TrainControlKeys.SLOTS * (SLOT_WIDTH + 2) + GAP;
    private static final int ASSIGN_WIDTH = 260;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFA0A0A0;
    private static final int ON_COLOR = 0xFFA3DF55;
    private static final int OFF_COLOR = 0xFFEE9246;
    private static final int FOCUS_FILL = 0x40FFFFFF;

    private record Row(int carriageNumber, @Nullable ControlTarget target, int y) {
    }

    private final UUID trainId;
    private final @Nullable ControlTarget focus;
    private boolean listMode;
    private final List<Row> rows = new ArrayList<>();
    private int hiddenRows;
    private int top;
    private int left;
    private int hintY;

    public TrainControlsScreen(UUID trainId, @Nullable ControlTarget focus, boolean listMode) {
        super(Component.translatable(TITLE));
        this.trainId = trainId;
        this.focus = focus;
        this.listMode = listMode || focus == null;
    }

    @Override
    protected void init() {
        rows.clear();
        hiddenRows = 0;
        if (listMode) {
            initList();
        } else {
            initAssign();
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
            .bounds(width / 2 - 50, hintY + 16, 100, BUTTON_HEIGHT)
            .build());
    }

    private void initAssign() {
        ControlTarget target = focus;
        left = width / 2 - ASSIGN_WIDTH / 2;
        top = Math.max(8, height / 2 - 96);
        int y = top + 32;
        for (int i = 0; i < TrainControlKeys.SLOTS; i++) {
            int slot = i;
            addRenderableWidget(Button.builder(slotLabel(i), button -> {
                    if (target != null) {
                        TrainControlKeys.assign(slot, target);
                    }
                    rebuildWidgets();
                })
                .bounds(left, y, ASSIGN_WIDTH, BUTTON_HEIGHT)
                .build());
            y += BUTTON_HEIGHT + 2;
        }
        y += GAP;
        addRenderableWidget(Button.builder(Component.translatable(SHOW_ALL), button -> {
                listMode = true;
                rebuildWidgets();
            })
            .bounds(left, y, ASSIGN_WIDTH, BUTTON_HEIGHT)
            .build());
        hintY = y + BUTTON_HEIGHT + GAP + 2;
    }

    private void initList() {
        left = width / 2 - LIST_WIDTH / 2;
        top = Math.max(8, height / 2 - 120);
        int y = top + 20;
        ClientLevel level = minecraft.level;
        Train train = level == null ? null : TrainControlKeys.train(level, trainId);
        if (train != null && level != null) {
            int total = 0;
            for (int index = 0; index < train.carriages.size(); index++) {
                CarriageContraptionEntity entity = TrainControlKeys.findCarriage(level, trainId, index);
                List<ControlTarget> found = entity == null ? null : TrainControlKeys.controlsOn(entity);
                if (found == null) {
                    if (total++ < MAX_ROWS) {
                        rows.add(new Row(index + 1, null, y));
                        y += ROW_HEIGHT;
                    }
                    continue;
                }
                for (ControlTarget target : found) {
                    if (total++ >= MAX_ROWS) {
                        continue;
                    }
                    rows.add(new Row(index + 1, target, y));
                    addRowButtons(target, y);
                    y += ROW_HEIGHT;
                }
            }
            hiddenRows = Math.max(0, total - MAX_ROWS);
        }
        if (rows.isEmpty()) {
            y += ROW_HEIGHT;
        }
        if (hiddenRows > 0) {
            y += 12;
        }
        hintY = y + GAP;
    }

    private void addRowButtons(ControlTarget target, int y) {
        int x = left + TEXT_WIDTH + STATE_WIDTH;
        addRenderableWidget(Button.builder(Component.translatable(SWITCH), button -> {
                if (minecraft.player != null && minecraft.level != null) {
                    TrainControlKeys.toggle(minecraft.player, minecraft.level, target);
                }
            })
            .bounds(x, y, SWITCH_WIDTH, BUTTON_HEIGHT)
            .build());
        x += SWITCH_WIDTH + GAP;
        int held = TrainControlKeys.slotOf(target);
        for (int i = 0; i < TrainControlKeys.SLOTS; i++) {
            int slot = i;
            Component label = Component.literal(String.valueOf(i + 1));
            if (held == i) {
                label = label.copy().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
            }
            addRenderableWidget(Button.builder(label, button -> {
                    TrainControlKeys.assign(slot, target);
                    rebuildWidgets();
                })
                .bounds(x, y, SLOT_WIDTH, BUTTON_HEIGHT)
                .build());
            x += SLOT_WIDTH + 2;
        }
    }

    private Component slotLabel(int index) {
        ControlTarget held = TrainControlKeys.slot(index);
        Component holder;
        if (held == null) {
            holder = Component.translatable(SLOT_EMPTY);
        } else if (focus != null && held.same(focus)) {
            holder = Component.translatable(SLOT_THIS).withStyle(ChatFormatting.GREEN);
        } else {
            holder = Component.translatable(HELD, held.actorName(), held.carriageNumber());
        }
        return Component.translatable(SLOT, index + 1, TrainControlKeys.keyName(index), holder);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        if (listMode && focus != null) {
            for (Row row : rows) {
                ControlTarget target = row.target();
                if (target != null && target.same(focus)) {
                    graphics.fill(left - 2, row.y() - 1, left + LIST_WIDTH + 2, row.y() + BUTTON_HEIGHT + 1, FOCUS_FILL);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (listMode) {
            renderList(graphics);
        } else {
            renderAssign(graphics);
        }
        graphics.drawCenteredString(font, hint(), width / 2, hintY, GREY);
    }

    private void renderAssign(GuiGraphics graphics) {
        graphics.drawCenteredString(font, title, width / 2, top, WHITE);
        ControlTarget target = focus;
        if (target != null) {
            graphics.drawCenteredString(font, Component.translatable(TARGET, target.actorName(), target.carriageNumber(), trainName()), width / 2, top + 14, GREY);
        }
    }

    private void renderList(GuiGraphics graphics) {
        graphics.drawCenteredString(font, Component.translatable(LIST_TITLE, trainName()), width / 2, top, WHITE);
        ClientLevel level = minecraft.level;
        if (rows.isEmpty()) {
            Component empty = level != null && TrainControlKeys.train(level, trainId) == null
                ? TrainControlMessages.refusal(TrainControlMessages.TRAIN_GONE)
                : Component.translatable(NONE);
            graphics.drawCenteredString(font, empty, width / 2, top + 26, GREY);
        }
        for (Row row : rows) {
            int textY = row.y() + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1;
            ControlTarget target = row.target();
            if (target == null) {
                graphics.drawString(font, Component.translatable(NOT_LOADED_ROW, row.carriageNumber()), left, textY, GREY);
                continue;
            }
            Component text = Component.translatable(ROW, row.carriageNumber(), target.actorName());
            String shown = font.plainSubstrByWidth(text.getString(), TEXT_WIDTH - GAP);
            graphics.drawString(font, shown, left, textY, WHITE);
            CarriageContraptionEntity entity = level == null ? null : TrainControlKeys.findCarriage(level, trainId, target.carriageIndex());
            if (entity != null) {
                boolean off = TrainControlKeys.isOff(entity, target);
                graphics.drawString(font, Component.translatable(off ? STATE_OFF : STATE_ON), left + TEXT_WIDTH, textY, off ? OFF_COLOR : ON_COLOR);
            }
        }
        if (hiddenRows > 0) {
            int y = rows.isEmpty() ? top + 20 : rows.getLast().y() + ROW_HEIGHT;
            graphics.drawCenteredString(font, Component.translatable(MORE, hiddenRows), width / 2, y, GREY);
        }
    }

    private Component trainName() {
        ClientLevel level = minecraft.level;
        Train train = level == null ? null : TrainControlKeys.train(level, trainId);
        return train == null || train.name == null ? Component.literal("?") : train.name;
    }

    private Component hint() {
        TrackwrightSettings settings = RemoteSettings.get();
        if (settings != null && !settings.contraptionControlsHotkeys()) {
            return SettingMessages.component(SettingMessages.HOTKEYS_OFF);
        }
        return Component.translatable(HINT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
