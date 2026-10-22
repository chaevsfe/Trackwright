package dev.chaevsfe.trackwright.client.guide;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class TrackGuideHud {
    private static final int BACKGROUND = 0xA0101010;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int EDGE = 6;
    private static final int PADDING = 3;
    private static final int LINE_HEIGHT = 10;
    private static final int CROSSHAIR_CLEARANCE = 12;
    private static final int MIN_WIDTH = 80;
    private static final int ACTION_BAR_CLEARANCE = 76;

    private TrackGuideHud() {
    }

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isSpectator() || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        List<Component> lines = TrackGuide.linesFor(player);
        if (lines.isEmpty()) {
            return;
        }
        Font font = mc.font;
        int x = EDGE + PADDING;
        int maxWidth = Math.max(MIN_WIDTH, graphics.guiWidth() / 2 - CROSSHAIR_CLEARANCE - PADDING - x);
        List<FormattedCharSequence> rows = new ArrayList<>();
        for (Component line : lines) {
            rows.addAll(font.split(line, maxWidth));
        }
        int width = 0;
        for (FormattedCharSequence row : rows) {
            width = Math.max(width, font.width(row));
        }
        int height = rows.size() * LINE_HEIGHT;
        int y = Math.max(PADDING + 1, Math.min((graphics.guiHeight() - height) / 2, graphics.guiHeight() - height - ACTION_BAR_CLEARANCE));
        graphics.fill(x - PADDING, y - PADDING, x + width + PADDING, y + height + PADDING - 2, BACKGROUND);
        for (FormattedCharSequence row : rows) {
            graphics.text(font, row, x, y, TEXT, true);
            y += LINE_HEIGHT;
        }
    }
}
