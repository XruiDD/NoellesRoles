package org.agmas.noellesroles.client.screen;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class RoleScreenHelper {
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
}
