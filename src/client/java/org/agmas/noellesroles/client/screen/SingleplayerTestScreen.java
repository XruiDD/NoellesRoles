package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.SingleplayerTestC2SPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SingleplayerTestScreen extends Screen {
    private static final List<String> GAME_MODES = List.of("wathe:murder", "wathe:discovery", "wathe:loose_ends");
    private static final List<String> MAP_EFFECTS = List.of("wathe:harpy_express_night", "wathe:harpy_express_day", "wathe:harpy_express_sundown", "wathe:generic");

    private final Screen parent;
    private final List<Role> roles = collectRoles();
    private TextFieldWidget startMinutesField;
    private int selectedRoleIndex;
    private int selectedGameModeIndex;
    private int selectedMapEffectIndex;

    public SingleplayerTestScreen(Screen parent) {
        super(Text.translatable("screen.noellesroles.singleplayer_test.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int panelWidth = 280;
        int left = centerX - panelWidth / 2;
        int y = 46;

        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("screen.noellesroles.singleplayer_test.role", displayRole(this.roles.get(this.selectedRoleIndex))),
            button -> {
                this.selectedRoleIndex = (this.selectedRoleIndex + 1) % this.roles.size();
                button.setMessage(Text.translatable("screen.noellesroles.singleplayer_test.role", displayRole(this.roles.get(this.selectedRoleIndex))));
            }
        ).dimensions(left, y, panelWidth, 20).build());

        y += 28;
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("screen.noellesroles.singleplayer_test.gamemode", GAME_MODES.get(this.selectedGameModeIndex)),
            button -> {
                this.selectedGameModeIndex = (this.selectedGameModeIndex + 1) % GAME_MODES.size();
                button.setMessage(Text.translatable("screen.noellesroles.singleplayer_test.gamemode", GAME_MODES.get(this.selectedGameModeIndex)));
            }
        ).dimensions(left, y, panelWidth, 20).build());

        y += 28;
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("screen.noellesroles.singleplayer_test.mapeffect", MAP_EFFECTS.get(this.selectedMapEffectIndex)),
            button -> {
                this.selectedMapEffectIndex = (this.selectedMapEffectIndex + 1) % MAP_EFFECTS.size();
                button.setMessage(Text.translatable("screen.noellesroles.singleplayer_test.mapeffect", MAP_EFFECTS.get(this.selectedMapEffectIndex)));
            }
        ).dimensions(left, y, panelWidth, 20).build());

        y += 28;
        this.startMinutesField = new TextFieldWidget(this.textRenderer, left, y, panelWidth, 20, Text.translatable("screen.noellesroles.singleplayer_test.minutes"));
        this.startMinutesField.setText("10");
        this.startMinutesField.setMaxLength(2);
        this.startMinutesField.setPlaceholder(Text.translatable("screen.noellesroles.singleplayer_test.minutes_hint"));
        this.addSelectableChild(this.startMinutesField);

        y += 32;
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("screen.noellesroles.singleplayer_test.start"),
            button -> this.sendTestRequest()
        ).dimensions(left, y, panelWidth, 20).build());

        y += 28;
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("screen.noellesroles.singleplayer_test.close"),
            button -> this.close()
        ).dimensions(left, y, panelWidth, 20).build());

        this.setInitialFocus(this.startMinutesField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 18, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.noellesroles.singleplayer_test.tip"), centerX, 30, 0xB8B8B8);

        if (this.startMinutesField != null) {
            this.startMinutesField.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    private void sendTestRequest() {
        int startMinutes = 10;
        try {
            startMinutes = Integer.parseInt(this.startMinutesField.getText().trim());
        } catch (NumberFormatException ignored) {
        }
        startMinutes = Math.max(1, Math.min(99, startMinutes));

        Role role = this.roles.get(this.selectedRoleIndex);
        ClientPlayNetworking.send(new SingleplayerTestC2SPacket(
            role.identifier().toString(),
            GAME_MODES.get(this.selectedGameModeIndex),
            MAP_EFFECTS.get(this.selectedMapEffectIndex),
            startMinutes
        ));

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("screen.noellesroles.singleplayer_test.sent", displayRole(role)), true);
        }
        this.close();
    }

    private static String displayRole(Role role) {
        return Text.translatable("announcement.role." + role.identifier().getPath()).getString();
    }

    private static List<Role> collectRoles() {
        List<Role> ret = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (WatheRoles.SPECIAL_ROLES.contains(role)) continue;
            if (Noellesroles.VANNILA_ROLES.contains(role)) continue;
            ret.add(role);
        }
        ret.sort(Comparator.comparing(role -> role.identifier().getPath()));
        return ret;
    }
}
