package org.agmas.noellesroles.client;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.widget.PlayerSelectWidget;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.packet.MorphC2SPacket;
import org.jetbrains.annotations.NotNull;

public class MorphlingPlayerWidget extends PlayerSelectWidget {
    private final AbstractClientPlayerEntity disguiseTarget;

    public MorphlingPlayerWidget(LimitedInventoryScreen screen, int x, int y, @NotNull AbstractClientPlayerEntity disguiseTarget, int index) {
        super(screen, x, y, disguiseTarget.getUuid(), disguiseTarget.getName(), (a) -> {
            if (MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player).getMorphTicks() == 0) {
                ClientPlayNetworking.send(new MorphC2SPacket(disguiseTarget.getUuid()));
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
        return MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player).getMorphTicks() < 0;
    }

    @Override
    protected int getCooldownTicks() {
        return -MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player).getMorphTicks();
    }

    @Override
    protected Text getHoverTooltip() {
        return disguiseTarget.getName();
    }
}
