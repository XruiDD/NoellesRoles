package org.agmas.noellesroles.client.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class RoleScreenHelper {
    public static final int MENU_CONTENT_SHIFT_Y = 10;
    public static final int MENU_BUTTON_Y_OFFSET = 42;

    private static final int MENU_TITLE_SHIFT_Y = 5;
    private static final int MENU_TITLE_OFFSET_Y = 115;
    private static final int MENU_SUBTITLE_GAP_Y = 15;
    private static final int MENU_VIEW_TOP_OFFSET = 72;
    private static final int MENU_VIEW_BOTTOM_GAP = 4;

    private RoleScreenHelper() {
    }

    public static void drawCenteredTitle(DrawContext context, TextRenderer font, Text text, int x, int y) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(1.5f, 1.5f, 1.5f);
        context.drawCenteredTextWithShadow(font, text, 0, 0, 0xFFFFFF);
        context.getMatrices().pop();
    }

    public static void drawCenteredSubTitle(DrawContext context, TextRenderer font, Text text, int x, int y) {
        context.drawCenteredTextWithShadow(font, text, x, y, 0xAAAAAA);
    }

    public static int getGridStartX(int itemCount, int columns, int spacingX, int centerX) {
        return centerX - ((Math.min(itemCount, columns) * spacingX) / 2) + 9;
    }

    public static int getGridStartY(int itemCount, int columns, int spacingY, int centerY) {
        int totalRows = (itemCount + columns - 1) / columns;
        return centerY - (totalRows * spacingY / 2) + 20;
    }

    public static int getMenuTitleY(int centerY) {
        return centerY - MENU_TITLE_OFFSET_Y - MENU_TITLE_SHIFT_Y;
    }

    public static int getMenuSubtitleY(int centerY) {
        return getMenuTitleY(centerY) + MENU_SUBTITLE_GAP_Y;
    }

    public static int getMenuStatusY(int centerY) {
        return getMenuSubtitleY(centerY) + MENU_SUBTITLE_GAP_Y;
    }

    public static int getMenuViewTop(int height) {
        return height / 2 - MENU_VIEW_TOP_OFFSET - MENU_TITLE_SHIFT_Y;
    }

    public static int getMenuViewBottom(int height) {
        return height - MENU_BUTTON_Y_OFFSET - MENU_VIEW_BOTTOM_GAP;
    }

    public static int getMenuButtonY(int height) {
        return height - MENU_BUTTON_Y_OFFSET;
    }
}
