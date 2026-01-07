package org.agmas.noellesroles.client.mixin.morphling;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.MorphlingPlayerWidget;
import org.agmas.noellesroles.client.widget.PlayerSelectWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;


@Mixin(LimitedInventoryScreen.class)
public abstract class MorphlingScreenMixin extends LimitedHandledScreen<PlayerScreenHandler>{
    @Shadow @Final public ClientPlayerEntity player;

    public MorphlingScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    void b(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player,Noellesroles.MORPHLING)) {
            List<UUID> lives = gameWorldComponent.getAllAlivePlayers();
            List<AbstractClientPlayerEntity> entries = MinecraftClient.getInstance().world.getPlayers();
            entries.removeIf(p -> !lives.contains(p.getUuid()) || p.getUuid().equals(player.getUuid()));

            for(int i = 0; i < entries.size(); ++i) {
                int x = PlayerSelectWidget.calculateGridX(((LimitedInventoryScreen)(Object)this).width, entries.size(), i);
                int y = PlayerSelectWidget.calculateGridY(((LimitedInventoryScreen)(Object)this).height, entries.size(), i);
                MorphlingPlayerWidget child = new MorphlingPlayerWidget(((LimitedInventoryScreen)(Object)this), x, y, entries.get(i), i);
                addDrawableChild(child);
            }
        }
    }
}
