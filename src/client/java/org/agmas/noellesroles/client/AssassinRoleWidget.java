package org.agmas.noellesroles.client;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.agmas.noellesroles.packet.AssassinGuessRoleC2SPacket;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AssassinRoleWidget extends ButtonWidget {
    public final Role role;

    // 预定义的颜色常量
    private static final int BG_COLOR_NORMAL = 0xAA000000;
    private static final int BG_COLOR_HOVER = 0xCC300000; // 悬停时深红色背景

    public AssassinRoleWidget(@Nullable LimitedInventoryScreen screen, int x, int y, Role role, UUID targetPlayer) {
        super(x, y, 90, 20, Text.empty(),
                (button) -> {
                    ClientPlayNetworking.send(new AssassinGuessRoleC2SPacket(targetPlayer, role.identifier()));
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.closeHandledScreen();
                    }
                }, DEFAULT_NARRATION_SUPPLIER);
        this.role = role;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHovered();

        // 1. 绘制背景 (黑色底，悬停变红)
        int bgColor = hovered ? BG_COLOR_HOVER : BG_COLOR_NORMAL;
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgColor);

        // 2. 绘制边框
        int borderColor = hovered ? 0xFFFF0000 : 0xFF444444; // 悬停红色，平时深灰
        context.drawBorder(getX(), getY(), getWidth(), getHeight(), borderColor);

        // 3. 绘制角色名称
        // 使用原版角色颜色，但悬停时强制变白以提高对比度
        int textColor = hovered ? 0xFFFFFF : role.color();
        Text roleName = Text.translatable("announcement.role." + role.identifier().getPath());

        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                roleName,
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                textColor
        );

        // 4. (可选) 绘制一个小角标装饰
        if (hovered) {
            context.fill(getX(), getY(), getX() + 2, getY() + 2, 0xFFFF0000);
            context.fill(getX() + getWidth() - 2, getY() + getHeight() - 2, getX() + getWidth(), getY() + getHeight(), 0xFFFF0000);
        }
    }
}