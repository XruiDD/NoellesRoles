package org.agmas.noellesroles.client;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import dev.doctor4t.wathe.client.WatheClient;
import org.agmas.noellesroles.client.widget.PlayerSelectWidget;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.packet.MorphC2SPacket;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class MorphlingPlayerWidget extends PlayerSelectWidget {
    private final UUID disguiseTargetUuid;

    public MorphlingPlayerWidget(LimitedInventoryScreen screen, int x, int y, @NotNull UUID disguiseTargetUuid, int index) {
        super(screen, x, y, disguiseTargetUuid, getNameText(disguiseTargetUuid), (a) -> {
            if (MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player).getMorphTicks() == 0) {
                ClientPlayNetworking.send(new MorphC2SPacket(disguiseTargetUuid));
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
        return MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player).getMorphTicks() < 0;
    }

    @Override
    protected int getCooldownTicks() {
        return -MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player).getMorphTicks();
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
