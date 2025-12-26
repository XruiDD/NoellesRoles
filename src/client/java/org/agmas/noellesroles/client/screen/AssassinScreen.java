package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.client.AssassinRoleWidget;
import org.agmas.noellesroles.client.AssassinTargetWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AssassinScreen extends Screen {
    private final ClientPlayerEntity player;
    private UUID selectedTarget = null;

    public AssassinScreen(ClientPlayerEntity player) {
        super(Text.translatable("screen.assassin.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(player);

        // 基础检查
        if (!gameWorld.isRole(player, Noellesroles.ASSASSIN) ||
                !GameFunctions.isPlayerAliveAndSurvival(player)) {
            this.close();
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (selectedTarget == null) {
            // === 阶段1：选择猎杀目标 ===

            // 获取目标列表
            List<UUID> alivePlayers = gameWorld.getAllAlivePlayers();
            List<AbstractClientPlayerEntity> targets = new ArrayList<>(MinecraftClient.getInstance().world.getPlayers());

            targets.removeIf(p -> {
                if (p.getUuid().equals(player.getUuid())) return true;
                if (!alivePlayers.contains(p.getUuid())) return true;
                if (gameWorld.isRole(p, WatheRoles.VIGILANTE)) return true;
                return false;
            });

            // 动态网格布局：每行最多 6 个人
            int columns = 6;
            int spacingX = 36; // 水平间距，与 SwapperScreenMixin 一致
            int spacingY = 45; // 垂直间距（稍大以留出名字空间）
            int totalRows = (int) Math.ceil((double) targets.size() / columns);
            int startX = centerX - ((Math.min(targets.size(), columns) * spacingX) / 2) + 9;
            int startY = centerY - (totalRows * spacingY / 2) + 20;

            for (int i = 0; i < targets.size(); i++) {
                AbstractClientPlayerEntity target = targets.get(i);
                int row = i / columns;
                int col = i % columns;

                AssassinTargetWidget widget = new AssassinTargetWidget(
                        null,
                        startX + col * spacingX,
                        startY + row * spacingY,
                        target,
                        i,
                        (selectedTarget) -> {
                            this.selectedTarget = selectedTarget;
                            this.clearAndInit();
                        }
                );
                addDrawableChild(widget);
            }
        } else {
            // === 阶段2：猜测身份 (处刑) ===

            List<Role> allRoles = getAllGuessableRoles();

            int columns = 3;
            int btnW = 90;
            int btnH = 20;
            int gapX = 5;
            int gapY = 5;

            int totalW = columns * btnW + (columns - 1) * gapX;
            int totalH = (int)Math.ceil((double)allRoles.size() / columns) * (btnH + gapY);

            int startX = centerX - totalW / 2;
            int startY = centerY - totalH / 2 + 10;

            for (int i = 0; i < allRoles.size(); i++) {
                int col = i % columns;
                int row = i / columns;

                AssassinRoleWidget widget = new AssassinRoleWidget(
                        null,
                        startX + col * (btnW + gapX),
                        startY + row * (btnH + gapY),
                        allRoles.get(i),
                        selectedTarget
                );
                addDrawableChild(widget);
            }

            // 返回/撤销按钮
            ButtonWidget backButton = ButtonWidget.builder(Text.translatable("screen.assassin.button.cancel"), (btn) -> {
                        selectedTarget = null;
                        this.clearAndInit();
                    })
                    .dimensions(centerX - 40, startY + totalH + 10, 80, 20)
                    .build();
            addDrawableChild(backButton);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制全屏深黑色半透明遮罩
        context.fill(0, 0, this.width, this.height, 0xF0000000);

        // 绘制上下红色的电影感边框
        context.fill(0, 0, this.width, 20, 0xFF500000);
        context.fill(0, this.height - 20, this.width, this.height, 0xFF500000);

        super.render(context, mouseX, mouseY, delta);

        // 渲染文字提示
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        if (selectedTarget == null) {
            // 阶段1 标题
            drawCenteredTitle(context, font, Text.translatable("screen.assassin.title.select_target"), centerX, centerY - 80);
            drawCenteredSubTitle(context, font, Text.translatable("screen.assassin.subtitle.click_to_confirm"), centerX, centerY - 65);
        } else {
            // 阶段2 标题
            PlayerEntity target = player.getWorld().getPlayerByUuid(selectedTarget);
            String targetName = target != null ? target.getName().getString() : Text.translatable("screen.assassin.unknown_target").getString();

            drawCenteredTitle(context, font, Text.translatable("screen.assassin.title.confirm_execution", targetName), centerX, centerY - 100);
            drawCenteredSubTitle(context, font, Text.translatable("screen.assassin.subtitle.warning"), centerX, centerY - 85);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        this.client.setScreen(null);
    }

    private void drawCenteredTitle(DrawContext context, TextRenderer font, Text text, int x, int y) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(1.5f, 1.5f, 1.5f);
        context.drawCenteredTextWithShadow(font, text, 0, 0, 0xFFFFFF);
        context.getMatrices().pop();
    }

    private void drawCenteredSubTitle(DrawContext context, TextRenderer font, Text text, int x, int y) {
        context.drawCenteredTextWithShadow(font, text, x, y, 0xAAAAAA);
    }

    private List<Role> getAllGuessableRoles() {
        List<Role> roles = new ArrayList<>();
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        for (Role role : WatheRoles.ROLES) {
            if (WatheRoles.SPECIAL_ROLES.contains(role)) continue;
            if (role.equals(WatheRoles.VIGILANTE)) continue;
            if (role.canUseKiller()) continue;
            if (!gameWorldComponent.isRoleEnabled(role)) continue;
            roles.add(role);
        }
        return roles;
    }
}
