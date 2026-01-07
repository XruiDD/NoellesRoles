package org.agmas.noellesroles.client.widget;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.UUID;

/**
 * 玩家选择Widget基类，用于巫毒师、变形者、交换者等角色的玩家选择界面
 */
public abstract class PlayerSelectWidget extends ButtonWidget {
    public static final int WIDGET_SIZE = 16;
    public static final int SLOT_SIZE = 30;
    public static final int SLOT_OFFSET = 7;
    public static final int APART = 36;
    public static final int MAX_PER_ROW = 12;

    protected final LimitedInventoryScreen screen;
    protected final UUID targetUUID;

    public PlayerSelectWidget(LimitedInventoryScreen screen, int x, int y, UUID targetUUID, Text message, PressAction onPress) {
        super(x, y, WIDGET_SIZE, WIDGET_SIZE, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.screen = screen;
        this.targetUUID = targetUUID;
    }

    /**
     * 获取目标玩家的皮肤纹理
     */
    protected abstract SkinTextures getSkinTextures();

    /**
     * 获取背景纹理类型
     */
    protected ShopEntry.Type getBackgroundType() {
        return ShopEntry.Type.TOOL;
    }

    /**
     * 是否处于冷却中
     */
    protected abstract boolean isOnCooldown();

    /**
     * 获取冷却剩余时间（tick）
     */
    protected abstract int getCooldownTicks();

    /**
     * 是否为选中状态
     */
    public boolean isSelected() {
        return false;
    }

    /**
     * 获取悬浮时显示的tooltip文字，返回null则不显示
     */
    protected Text getHoverTooltip() {
        return null;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        boolean onCooldown = isOnCooldown();

        if (onCooldown) {
            context.setShaderColor(0.25f, 0.25f, 0.25f, 0.5f);
        }

        // 绘制背景纹理
        context.drawGuiTexture(getBackgroundType().getTexture(), this.getX() - SLOT_OFFSET, this.getY() - SLOT_OFFSET, SLOT_SIZE, SLOT_SIZE);

        // 绘制玩家头像
        SkinTextures skin = getSkinTextures();
        if (skin != null) {
            PlayerSkinDrawer.draw(context, skin.texture(), this.getX(), this.getY(), WIDGET_SIZE);
        }

        // 悬浮高亮
        if (this.isHovered()) {
            drawSlotHighlight(context, this.getX(), this.getY(), 0);
            Text tooltip = getHoverTooltip();
            if (tooltip != null) {
                int tooltipX = this.getX() + 8 - MinecraftClient.getInstance().textRenderer.getWidth(tooltip) / 2;
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, tooltip, tooltipX, this.getY() - 12, Color.WHITE.getRGB());
            }
        }

        // 选中状态
        if (isSelected()) {
            Text selectedText = Text.literal("Selected");
            int textX = this.getX() - 4 - MinecraftClient.getInstance().textRenderer.getWidth(selectedText) / 2;
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, selectedText, textX, this.getY() - 9);
            drawSlotHighlight(context, this.getX(), this.getY(), 0);
        }

        if (onCooldown) {
            context.setShaderColor(1f, 1f, 1f, 1f);
            // 显示冷却时间
            int cooldownSeconds = getCooldownTicks() / 20;
            context.drawText(MinecraftClient.getInstance().textRenderer, cooldownSeconds + "", this.getX(), this.getY(), Color.RED.getRGB(), true);
        }
    }

    /**
     * 绘制槽位高亮效果
     */
    protected void drawSlotHighlight(DrawContext context, int x, int y, int z) {
        int color = -1862287543;
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, z);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        // 不绘制按钮文字
    }

    /**
     * 计算网格布局中的X坐标
     * @param screenWidth 屏幕宽度
     * @param totalCount 总玩家数
     * @param index 当前索引
     * @return X坐标
     */
    public static int calculateGridX(int screenWidth, int totalCount, int index) {
        int row = index / MAX_PER_ROW;
        int col = index % MAX_PER_ROW;
        int countInRow = Math.min(totalCount - row * MAX_PER_ROW, MAX_PER_ROW);
        return screenWidth / 2 - countInRow * APART / 2 + 9 + col * APART;
    }

    /**
     * 计算网格布局中的Y坐标
     * @param screenHeight 屏幕高度
     * @param totalCount 总玩家数
     * @param index 当前索引
     * @return Y坐标
     */
    public static int calculateGridY(int screenHeight, int totalCount, int index) {
        int totalRows = (totalCount + MAX_PER_ROW - 1) / MAX_PER_ROW;
        int row = index / MAX_PER_ROW;
        int baseY = (screenHeight - 32) / 2 + 90 - (totalRows - 1) * APART / 2;
        return baseY + row * APART;
    }
}
