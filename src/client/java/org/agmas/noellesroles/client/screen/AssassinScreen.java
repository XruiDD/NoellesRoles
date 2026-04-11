package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.client.AssassinRoleWidget;
import org.agmas.noellesroles.client.AssassinTargetWidget;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AssassinScreen extends Screen {
    private static final int TOP_BAR_HEIGHT = 20;
    private static final int BOTTOM_BAR_HEIGHT = 20;
    private static final int BACKGROUND_OVERLAY_COLOR = 0xB0000000;
    private static final int ACCENT_BAR_COLOR = 0xFF500000;
    private static final int ROLE_COLUMNS = 3;
    private static final int ROLE_BUTTON_WIDTH = 90;
    private static final int ROLE_BUTTON_HEIGHT = 20;
    private static final int ROLE_GAP_X = 5;
    private static final int ROLE_GAP_Y = 5;
    private static final double ROLE_SCROLL_STEP = ROLE_BUTTON_HEIGHT + ROLE_GAP_Y;
    private static final int TARGET_COLUMNS = 6;
    private static final int TARGET_SPACING_X = 36;
    private static final int TARGET_SPACING_Y = 45;
    private static final double TARGET_SCROLL_STEP = 24.0;

    private final ClientPlayerEntity player;
    private final List<AssassinRoleWidget> roleWidgets = new ArrayList<>();
    private UUID selectedTarget = null;
    private ButtonWidget backButton = null;
    private ButtonWidget targetCloseButton = null;
    private int targetScrollOffset = 0;
    private int targetMaxScroll = 0;
    private int roleListStartX;
    private int roleListBaseY;
    private int roleViewportTop;
    private int roleViewportBottom;
    private int roleContentHeight;
    private double roleScrollOffset = 0.0;
    public AssassinScreen(ClientPlayerEntity player) {
        super(Text.translatable("screen.assassin.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        roleWidgets.clear();
        backButton = null;
        targetCloseButton = null;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        AssassinPlayerComponent assassinComp = AssassinPlayerComponent.KEY.get(player);

        // 基础检查
        if (!gameWorld.isRole(player, Noellesroles.ASSASSIN) ||
                !GameFunctions.isPlayerPlayingAndAlive(player) || SwallowedPlayerComponent.isPlayerSwallowed(MinecraftClient.getInstance().player)) {
            this.close();
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (selectedTarget == null) {
            targetCloseButton = ButtonWidget.builder(Text.translatable("screen.criminal_reasoner.button.close"), btn -> this.close())
                    .dimensions(centerX - 40, RoleScreenHelper.getMenuButtonY(this.height), 80, 20)
                    .build();
            addDrawableChild(targetCloseButton);
            // === 阶段1：选择猎杀目标 ===

            // 获取目标列表
            List<UUID> alivePlayers = gameWorld.getAllAlivePlayers();
            List<UUID> targets = new ArrayList<>(WatheClient.PLAYER_ENTRIES_CACHE.keySet());
            targets.removeIf(uuid -> uuid.equals(player.getUuid()) || !alivePlayers.contains(uuid));

            // 动态网格布局：每行最多 6 个人
            int columns = 6;
            int spacingX = 36; // 水平间距，与 SwapperScreenMixin 一致
            int spacingY = 45; // 垂直间距（稍大以留出名字空间）
            int rows = Math.max(1, (targets.size() + TARGET_COLUMNS - 1) / TARGET_COLUMNS);
            int contentHeight = rows * TARGET_SPACING_Y + RoleScreenHelper.MENU_CONTENT_SHIFT_Y;
            int viewTop = getTargetViewTop();
            int viewBottom = getTargetViewBottom();
            int viewHeight = Math.max(1, viewBottom - viewTop);
            targetMaxScroll = Math.max(0, contentHeight - viewHeight);
            targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, targetMaxScroll));
            int startX = RoleScreenHelper.getGridStartX(targets.size(), TARGET_COLUMNS, TARGET_SPACING_X, centerX);
            int startY = viewTop + RoleScreenHelper.MENU_CONTENT_SHIFT_Y - targetScrollOffset;

            for (int i = 0; i < targets.size(); i++) {
                UUID targetUuid = targets.get(i);
                int row = i / TARGET_COLUMNS;
                int col = i % TARGET_COLUMNS;

                AssassinTargetWidget widget = new AssassinTargetWidget(
                        null,
                        startX + col * TARGET_SPACING_X,
                        startY + row * TARGET_SPACING_Y,
                        targetUuid,
                        i,
                        (selectedTarget) -> {
                            this.selectedTarget = selectedTarget;
                            this.clearAndInit();
                        }
                );
                widget.visible = widget.getY() + 16 > viewTop && widget.getY() < viewBottom;
                addDrawableChild(widget);
            }
        } else {
            // === 阶段2：猜测身份 (处刑) ===

            List<Role> allRoles = getAllGuessableRoles();

            int rows = (int) Math.ceil((double) allRoles.size() / ROLE_COLUMNS);
            int totalW = ROLE_COLUMNS * ROLE_BUTTON_WIDTH + (ROLE_COLUMNS - 1) * ROLE_GAP_X;

            roleListStartX = centerX - totalW / 2;
            roleViewportTop = Math.max(42, centerY - 74);
            int backButtonY = this.height - 42;
            roleViewportBottom = Math.max(roleViewportTop + ROLE_BUTTON_HEIGHT, backButtonY - 4);
            roleListBaseY = roleViewportTop;
            roleContentHeight = Math.max(0, rows * ROLE_BUTTON_HEIGHT + Math.max(0, rows - 1) * ROLE_GAP_Y);
            roleScrollOffset = Math.max(0.0, Math.min(roleScrollOffset, getMaxRoleScroll()));

            for (int i = 0; i < allRoles.size(); i++) {
                int col = i % ROLE_COLUMNS;
                int row = i / ROLE_COLUMNS;

                AssassinRoleWidget widget = new AssassinRoleWidget(
                        null,
                        roleListStartX + col * (ROLE_BUTTON_WIDTH + ROLE_GAP_X),
                        roleListBaseY + row * (ROLE_BUTTON_HEIGHT + ROLE_GAP_Y),
                        allRoles.get(i),
                        selectedTarget
                );
                roleWidgets.add(widget);
                addDrawableChild(widget);
            }

            // 返回/撤销按钮
            backButton = ButtonWidget.builder(Text.translatable("screen.assassin.button.cancel"), (btn) -> {
                        selectedTarget = null;
                        this.clearAndInit();
                    })
                    .dimensions(centerX - 40, backButtonY, 80, 20)
                    .build();
            addDrawableChild(backButton);
            updateRoleWidgetPositions();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制上下红色的电影感边框
        renderBackground(context, mouseX, mouseY, delta);

        if (selectedTarget == null) {
            super.render(context, mouseX, mouseY, delta);
            if (targetCloseButton != null) {
                targetCloseButton.render(context, mouseX, mouseY, delta);
            }
        } else {
            updateRoleWidgetPositions();
            context.enableScissor(0, roleViewportTop, this.width, roleViewportBottom);
            for (AssassinRoleWidget widget : roleWidgets) {
                if (widget.visible) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            }
            context.disableScissor();
            if (backButton != null) {
                backButton.render(context, mouseX, mouseY, delta);
            }
        }

        // 渲染文字提示
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        if (selectedTarget == null) {
            // 阶段1 标题
            RoleScreenHelper.drawCenteredTitle(context, font, Text.translatable("screen.assassin.title.select_target"), centerX, RoleScreenHelper.getMenuTitleY(centerY));
            RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.assassin.subtitle.click_to_confirm"), centerX, RoleScreenHelper.getMenuSubtitleY(centerY));
        } else {
            // 阶段2 标题
            PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(selectedTarget);
            Text displayName = entry != null ? entry.getDisplayName() : null;
            String targetName = displayName != null ? displayName.getString()
                : entry != null ? entry.getProfile().getName()
                : Text.translatable("screen.assassin.unknown_target").getString();

            RoleScreenHelper.drawCenteredTitle(context, font, Text.translatable("screen.assassin.title.confirm_execution", targetName), centerX, centerY - 118);
            RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.assassin.subtitle.warning"), centerX, centerY - 102);
        }
        context.fill(0, 0, this.width, TOP_BAR_HEIGHT, ACCENT_BAR_COLOR);
        context.fill(0, this.height - BOTTOM_BAR_HEIGHT, this.width, this.height, ACCENT_BAR_COLOR);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selectedTarget == null && targetMaxScroll > 0) {
            double nextOffset = targetScrollOffset - verticalAmount * TARGET_SCROLL_STEP;
            targetScrollOffset = Math.max(0, Math.min((int) Math.round(nextOffset), targetMaxScroll));
            this.clearAndInit();
            return true;
        }
        if (selectedTarget != null && mouseY >= roleViewportTop && mouseY <= roleViewportBottom) {
            double nextOffset = roleScrollOffset - verticalAmount * ROLE_SCROLL_STEP;
            roleScrollOffset = Math.max(0.0, Math.min(nextOffset, getMaxRoleScroll()));
            updateRoleWidgetPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, BACKGROUND_OVERLAY_COLOR);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        this.client.setScreen(null);
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

    private void updateRoleWidgetPositions() {
        for (int i = 0; i < roleWidgets.size(); i++) {
            AssassinRoleWidget widget = roleWidgets.get(i);
            int col = i % ROLE_COLUMNS;
            int row = i / ROLE_COLUMNS;
            int x = roleListStartX + col * (ROLE_BUTTON_WIDTH + ROLE_GAP_X);
            int y = roleListBaseY + row * (ROLE_BUTTON_HEIGHT + ROLE_GAP_Y) - (int) Math.round(roleScrollOffset);
            widget.setPosition(x, y);
            widget.visible = y + ROLE_BUTTON_HEIGHT > roleViewportTop && y < roleViewportBottom;
        }
    }

    private double getMaxRoleScroll() {
        return Math.max(0, roleContentHeight - (roleViewportBottom - roleViewportTop));
    }

    private int getTargetViewTop() {
        return RoleScreenHelper.getMenuViewTop(this.height);
    }

    private int getTargetViewBottom() {
        return RoleScreenHelper.getMenuViewBottom(this.height);
    }
}
