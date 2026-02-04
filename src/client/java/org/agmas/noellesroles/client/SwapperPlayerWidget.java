package org.agmas.noellesroles.client;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.DefaultSkinHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.text.Text;
import dev.doctor4t.wathe.client.WatheClient;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.widget.PlayerSelectWidget;
import org.agmas.noellesroles.packet.SwapperC2SPacket;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SwapperPlayerWidget extends PlayerSelectWidget {
    private final UUID disguiseTargetUuid;

    public static UUID playerChoiceOne = null;

    public SwapperPlayerWidget(LimitedInventoryScreen screen, int x, int y, @NotNull UUID disguiseTargetUuid, int index) {
        super(screen, x, y, disguiseTargetUuid, getNameText(disguiseTargetUuid), (a) -> {
            if (AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player).cooldown == 0) {
                if (MinecraftClient.getInstance().player.getWorld().getPlayerByUuid(disguiseTargetUuid) == null) return;
                if (MinecraftClient.getInstance().player.getWorld().getPlayerByUuid(disguiseTargetUuid).hasVehicle()) return;
                if (playerChoiceOne != null) {
                    ClientPlayNetworking.send(new SwapperC2SPacket(playerChoiceOne, disguiseTargetUuid));
                } else {
                    playerChoiceOne = disguiseTargetUuid;
                }
            }
        });
        this.disguiseTargetUuid = disguiseTargetUuid;
    }

    @Override
    protected SkinTextures getSkinTextures() {
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(disguiseTargetUuid);
        if (entry != null) {
            return entry.getSkinTextures();
        }
        return DefaultSkinHelper.getSkinTextures(new GameProfile(disguiseTargetUuid, "Unknown"));
    }

    @Override
    protected ShopEntry.Type getBackgroundType() {
        return ShopEntry.Type.POISON;
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
    protected Text getHoverTooltip() {
        return getNameText(disguiseTargetUuid);
    }

    private static Text getNameText(UUID targetUuid) {
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(targetUuid);
        if (entry != null && entry.getDisplayName() != null) {
            return entry.getDisplayName();
        }
        return entry != null ? Text.literal(entry.getProfile().getName()) : Text.literal("Unknown");
    }
}
