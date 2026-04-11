package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.CommanderTargetWidget;
import org.agmas.noellesroles.commander.CommanderPlayerComponent;
import org.agmas.noellesroles.packet.CommanderMarkC2SPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommanderScreen extends Screen {
    private static final int TOP_BAR_HEIGHT = 20;
    private static final int BOTTOM_BAR_HEIGHT = 20;
    private static final int BACKGROUND_OVERLAY_COLOR = 0xB0000000;
    private static final int COLUMNS = 6;
    private static final int SPACING_X = 36;
    private static final int SPACING_Y = 45;
    private static final double SCROLL_STEP = 24.0;

    private final ClientPlayerEntity player;
    private int scrollOffset;
    private int maxScroll;

    public CommanderScreen(ClientPlayerEntity player) {
        super(Text.translatable("screen.commander.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(this.player);
        CommanderPlayerComponent commanderComp = CommanderPlayerComponent.KEY.get(this.player);
        if (!gameWorld.isRole(this.player, Noellesroles.COMMANDER)
                || !GameFunctions.isPlayerPlayingAndAlive(this.player)
                || SwallowedPlayerComponent.isPlayerSwallowed(this.player)
                || ability.getCooldown() > 0
                || !commanderComp.canMarkMore()) {
            this.close();
            return;
        }

        List<UUID> targets = new ArrayList<>(WatheClient.PLAYER_ENTRIES_CACHE.keySet());
        List<UUID> alivePlayers = gameWorld.getAllAlivePlayers();
        targets.removeIf(uuid -> uuid.equals(this.player.getUuid())
                || !alivePlayers.contains(uuid)
                || commanderComp.isThreatTarget(uuid));

        int centerX = this.width / 2;
        int rows = Math.max(1, (targets.size() + COLUMNS - 1) / COLUMNS);
        int contentHeight = rows * SPACING_Y + RoleScreenHelper.MENU_CONTENT_SHIFT_Y;
        int viewTop = RoleScreenHelper.getMenuViewTop(this.height);
        int viewBottom = RoleScreenHelper.getMenuViewBottom(this.height);
        int viewHeight = Math.max(1, viewBottom - viewTop);
        maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        int startX = RoleScreenHelper.getGridStartX(targets.size(), COLUMNS, SPACING_X, centerX);
        int startY = viewTop + RoleScreenHelper.MENU_CONTENT_SHIFT_Y - scrollOffset;

        for (int i = 0; i < targets.size(); i++) {
            UUID targetUuid = targets.get(i);
            int row = i / COLUMNS;
            int col = i % COLUMNS;
            CommanderTargetWidget widget = new CommanderTargetWidget(
                    startX + col * SPACING_X,
                    startY + row * SPACING_Y,
                    targetUuid,
                    selectedUuid -> {
                        ClientPlayNetworking.send(new CommanderMarkC2SPacket(selectedUuid));
                        this.close();
                    });
            widget.visible = widget.getY() + 16 > viewTop && widget.getY() < viewBottom;
            addDrawableChild(widget);
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.commander.button.close"), button -> this.close())
                .dimensions(centerX - 40, RoleScreenHelper.getMenuButtonY(this.height), 80, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int accentColor = 0xFF2E006B;
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        CommanderPlayerComponent commanderComp = CommanderPlayerComponent.KEY.get(this.player);

        RoleScreenHelper.drawCenteredTitle(context, font, Text.translatable("screen.commander.title.select_target"), centerX, RoleScreenHelper.getMenuTitleY(centerY));
        RoleScreenHelper.drawCenteredSubTitle(context, font,
                Text.translatable("screen.commander.subtitle.remaining", commanderComp.getRemainingMarks()),
                centerX, RoleScreenHelper.getMenuSubtitleY(centerY));

        if (children().size() <= 1) {
            RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.commander.empty"), centerX, centerY);
        }

        List<String> markedNames = commanderComp.getThreatTargetNames();
        if (!markedNames.isEmpty()) {
            String joined = String.join(" / ", markedNames);
            RoleScreenHelper.drawCenteredSubTitle(context, font, Text.translatable("screen.commander.current_targets", joined), centerX, RoleScreenHelper.getMenuStatusY(centerY));
        }

        context.fill(0, 0, this.width, TOP_BAR_HEIGHT, accentColor);
        context.fill(0, this.height - BOTTOM_BAR_HEIGHT, this.width, this.height, accentColor);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            int nextOffset = scrollOffset - (int) Math.round(verticalAmount * SCROLL_STEP);
            nextOffset = Math.max(0, Math.min(nextOffset, maxScroll));
            if (nextOffset != scrollOffset) {
                scrollOffset = nextOffset;
                this.clearAndInit();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, BACKGROUND_OVERLAY_COLOR);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

}
