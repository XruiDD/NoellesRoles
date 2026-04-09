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
    private static final int COLUMNS = 6;
    private static final int SPACING_X = 36;
    private static final int SPACING_Y = 45;

    private final ClientPlayerEntity player;

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
        int centerY = this.height / 2;
        int rows = (int) Math.ceil((double) Math.max(1, targets.size()) / COLUMNS);
        int startX = centerX - ((Math.min(Math.max(targets.size(), 1), COLUMNS) * SPACING_X) / 2) + 9;
        int startY = centerY - (rows * SPACING_Y / 2) + 20;

        for (int i = 0; i < targets.size(); i++) {
            UUID targetUuid = targets.get(i);
            int row = i / COLUMNS;
            int col = i % COLUMNS;
            addDrawableChild(new CommanderTargetWidget(
                    startX + col * SPACING_X,
                    startY + row * SPACING_Y,
                    targetUuid,
                    selectedUuid -> {
                        ClientPlayNetworking.send(new CommanderMarkC2SPacket(selectedUuid));
                        this.close();
                    }
            ));
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.commander.button.close"), button -> this.close())
                .dimensions(centerX - 40, this.height - 42, 80, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xF0000000);
        int accentColor = 0xFF1B2C58;
        context.fill(0, 0, this.width, 20, accentColor);
        context.fill(0, this.height - 20, this.width, this.height, accentColor);

        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        CommanderPlayerComponent commanderComp = CommanderPlayerComponent.KEY.get(this.player);

        drawCenteredTitle(context, font, Text.translatable("screen.commander.title.select_target"), centerX, centerY - 92);
        drawCenteredSubTitle(context, font,
                Text.translatable("screen.commander.subtitle.remaining", commanderComp.getRemainingMarks()),
                centerX, centerY - 76);

        if (children().size() <= 1) {
            drawCenteredSubTitle(context, font, Text.translatable("screen.commander.empty"), centerX, centerY);
        }

        List<String> markedNames = commanderComp.getThreatTargetNames();
        if (!markedNames.isEmpty()) {
            String joined = String.join(" / ", markedNames);
            drawCenteredSubTitle(context, font, Text.translatable("screen.commander.current_targets", joined), centerX, this.height - 64);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    private void drawCenteredTitle(DrawContext context, TextRenderer font, Text text, int x, int y) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(1.5f, 1.5f, 1.5f);
        context.drawCenteredTextWithShadow(font, text, 0, 0, 0xFFFFFF);
        context.getMatrices().pop();
    }

    private void drawCenteredSubTitle(DrawContext context, TextRenderer font, Text text, int x, int y) {
        context.drawCenteredTextWithShadow(font, text, x, y, 0xC8D2EA);
    }
}
