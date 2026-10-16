package dev.chaevsfe.trackwright.client.guide;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TrackGuideHud {
    private static final int BACKGROUND = 0xA0101010;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int EDGE = 6;
    private static final int PADDING = 3;
    private static final int LINE_HEIGHT = 10;

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
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        int height = lines.size() * LINE_HEIGHT;
        int x = EDGE + PADDING;
        int y = Math.max(EDGE + PADDING, (graphics.guiHeight() - height) / 2);
        graphics.fill(x - PADDING, y - PADDING, x + width + PADDING, y + height + PADDING - 2, BACKGROUND);
        for (Component line : lines) {
            graphics.text(font, line, x, y, TEXT, true);
            y += LINE_HEIGHT;
        }
    }
}
