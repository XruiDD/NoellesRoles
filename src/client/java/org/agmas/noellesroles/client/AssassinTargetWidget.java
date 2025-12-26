package org.agmas.noellesroles.client;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.UUID;
import java.util.function.Consumer;

public class AssassinTargetWidget extends ButtonWidget {
    @Nullable
    public final LimitedInventoryScreen screen;
    public final AbstractClientPlayerEntity target;

    public AssassinTargetWidget(@Nullable LimitedInventoryScreen screen, int x, int y, @NotNull AbstractClientPlayerEntity target, int index, Consumer<UUID> onTargetSelected) {
        super(x, y, 16, 16, target.getName(), (button) -> {
            AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
            if (assassinComp.canGuess()) {
                // 调用回调函数，通知Screen更新客户端状态
                onTargetSelected.accept(target.getUuid());
            }
        }, DEFAULT_NARRATION_SUPPLIER);
        this.screen = screen;
        this.target = target;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(MinecraftClient.getInstance().player);

        if (!assassinComp.canGuess()) {
            // 冷却中或次数用完：灰色显示
            context.setShaderColor(0.25f, 0.25f, 0.25f, 0.5f);
            context.drawGuiTexture(ShopEntry.Type.POISON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
            PlayerSkinDrawer.draw(context, target.getSkinTextures().texture(), this.getX(), this.getY(), 16);
            context.setShaderColor(1f, 1f, 1f, 1f);

            // 显示冷却倒计时
            if (assassinComp.getCooldownTicks() > 0) {
                String cooldown = String.valueOf(assassinComp.getCooldownTicks() / 20);
                context.drawText(MinecraftClient.getInstance().textRenderer, cooldown,
                    this.getX(), this.getY(), Color.RED.getRGB(), true);
            }

            if (this.isHovered()) {
                this.drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
                context.drawTooltip(MinecraftClient.getInstance().textRenderer, target.getName(),
                    this.getX() - 4 - MinecraftClient.getInstance().textRenderer.getWidth(target.getName()) / 2,
                    this.getY() - 9);
            }
        } else {
            // 可用：正常显示
            context.drawGuiTexture(ShopEntry.Type.POISON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
            PlayerSkinDrawer.draw(context, target.getSkinTextures().texture(), this.getX(), this.getY(), 16);

            if (this.isHovered()) {
                this.drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
                context.drawTooltip(MinecraftClient.getInstance().textRenderer, target.getName(),
                    this.getX() - 4 - MinecraftClient.getInstance().textRenderer.getWidth(target.getName()) / 2,
                    this.getY() - 9);
            }
        }
    }

    private void drawShopSlotHighlight(DrawContext context, int x, int y, int z) {
        int color = -1862287543; // 高亮颜色
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, z);
    }

    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
    }
}
