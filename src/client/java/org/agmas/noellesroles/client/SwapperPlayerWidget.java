package org.agmas.noellesroles.client;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.widget.PlayerSelectWidget;
import org.agmas.noellesroles.packet.SwapperC2SPacket;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SwapperPlayerWidget extends PlayerSelectWidget {
    private final AbstractClientPlayerEntity disguiseTarget;

    public static UUID playerChoiceOne = null;

    public SwapperPlayerWidget(LimitedInventoryScreen screen, int x, int y, @NotNull AbstractClientPlayerEntity disguiseTarget, int index) {
        super(screen, x, y, disguiseTarget.getUuid(), disguiseTarget.getName(), (a) -> {
            if (AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player).cooldown == 0) {
                if (MinecraftClient.getInstance().player.getWorld().getPlayerByUuid(disguiseTarget.getUuid()) == null) return;
                if (MinecraftClient.getInstance().player.getWorld().getPlayerByUuid(disguiseTarget.getUuid()).hasVehicle()) return;
                if (playerChoiceOne != null) {
                    ClientPlayNetworking.send(new SwapperC2SPacket(playerChoiceOne, disguiseTarget.getUuid()));
                } else {
                    playerChoiceOne = disguiseTarget.getUuid();
                }
            }
        });
        this.disguiseTarget = disguiseTarget;
    }

    @Override
    protected SkinTextures getSkinTextures() {
        return disguiseTarget.getSkinTextures();
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
        return disguiseTarget.getName();
    }
}
