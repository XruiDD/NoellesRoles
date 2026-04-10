package org.agmas.noellesroles.client;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.agmas.noellesroles.commander.CommanderPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public class CommanderTargetWidget extends ButtonWidget {
    private final UUID targetUuid;
    private final Consumer<UUID> onTargetSelected;

    public CommanderTargetWidget(int x, int y, UUID targetUuid, Consumer<UUID> onTargetSelected) {
        super(x, y, 16, 16, getNameText(targetUuid), button -> onTargetSelected.accept(targetUuid), DEFAULT_NARRATION_SUPPLIER);
        this.targetUuid = targetUuid;
        this.onTargetSelected = onTargetSelected;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        CommanderPlayerComponent commanderComp = CommanderPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        boolean marked = commanderComp.isThreatTarget(this.targetUuid);

        context.drawGuiTexture(ShopEntry.Type.WEAPON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        PlayerSkinDrawer.draw(context, getSkinTextures().texture(), this.getX(), this.getY(), 16);

        if (marked || this.isHovered()) {
            drawSlotHighlight(context, this.getX(), this.getY(), 0, marked ? 0xAA4B1A8E : 0x913D3D3D);
            Text name = getNameText(this.targetUuid);
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, name,
                    this.getX() - 4 - MinecraftClient.getInstance().textRenderer.getWidth(name) / 2,
                    this.getY() - 9);
        }
    }

    private void drawSlotHighlight(DrawContext context, int x, int y, int z, int color) {
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, z);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
    }

    private static Text getNameText(UUID targetUuid) {
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(targetUuid);
        if (entry != null && entry.getDisplayName() != null) {
            return entry.getDisplayName();
        }
        return entry != null ? Text.literal(entry.getProfile().getName()) : Text.literal("Unknown");
    }

    private SkinTextures getSkinTextures() {
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(this.targetUuid);
        if (entry != null) {
            return entry.getSkinTextures();
        }
        return DefaultSkinHelper.getSkinTextures(new GameProfile(this.targetUuid, "Unknown"));
    }
}
