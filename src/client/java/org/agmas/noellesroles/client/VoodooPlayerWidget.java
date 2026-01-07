package org.agmas.noellesroles.client;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.widget.PlayerSelectWidget;
import org.agmas.noellesroles.packet.MorphC2SPacket;
import org.agmas.noellesroles.voodoo.VoodooPlayerComponent;

import java.util.UUID;

public class VoodooPlayerWidget extends PlayerSelectWidget {
    private final PlayerListEntry targetPlayerEntry;

    public VoodooPlayerWidget(LimitedInventoryScreen screen, int x, int y, UUID targetUUID, PlayerListEntry targetPlayerEntry, World world, int index) {
        super(screen, x, y, targetUUID, Text.literal(""), (a) -> {
            ClientPlayNetworking.send(new MorphC2SPacket(targetUUID));
        });
        this.targetPlayerEntry = targetPlayerEntry;
    }

    @Override
    protected SkinTextures getSkinTextures() {
        return targetPlayerEntry != null ? targetPlayerEntry.getSkinTextures() : null;
    }

    @Override
    protected ShopEntry.Type getBackgroundType() {
        return ShopEntry.Type.TOOL;
    }

    @Override
    protected boolean isOnCooldown() {
        return AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player).cooldown > 0;
    }

    @Override
    protected int getCooldownTicks() {
        return AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player).cooldown;
    }

    @Override
    public boolean isSelected() {
        VoodooPlayerComponent voodooComponent = VoodooPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        return voodooComponent.target.equals(targetUUID);
    }

    @Override
    protected Text getHoverTooltip() {
        return targetPlayerEntry != null ? targetPlayerEntry.getProfile().getName() != null
            ? Text.literal(targetPlayerEntry.getProfile().getName()) : null : null;
    }
}
