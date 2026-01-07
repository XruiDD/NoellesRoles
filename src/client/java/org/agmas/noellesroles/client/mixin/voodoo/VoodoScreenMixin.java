package org.agmas.noellesroles.client.mixin.voodoo;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.VoodooPlayerWidget;
import org.agmas.noellesroles.client.widget.PlayerSelectWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.List;
import java.util.UUID;


@Mixin(LimitedInventoryScreen.class)
public abstract class VoodoScreenMixin extends LimitedHandledScreen<PlayerScreenHandler>{
    @Shadow @Final public ClientPlayerEntity player;

    public VoodoScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    @Inject(method = "render", at = @At("HEAD"))
    void a(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        ConfigWorldComponent configWorldComponent = (ConfigWorldComponent) ConfigWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player,Noellesroles.VOODOO)) {
            int y = (height- 32) / 2;
            int x = width / 2;
            if (!configWorldComponent.naturalVoodoosAllowed) {
                Text text = Text.translatable("hud.voodoo.natural_death_disabled");
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, x - (MinecraftClient.getInstance().textRenderer.getWidth(text)/2), y + 35, Color.RED.getRGB());
            }
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    void b(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player,Noellesroles.VOODOO)) {
            List<UUID> entries = gameWorldComponent.getAllPlayers();
            entries.removeIf((e) -> e.equals(player.getUuid()));

            for(int i = 0; i < entries.size(); ++i) {
                PlayerListEntry playerEntry = MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(entries.get(i));
                if (playerEntry == null) continue;
                int x = PlayerSelectWidget.calculateGridX(width, entries.size(), i);
                int y = PlayerSelectWidget.calculateGridY(height, entries.size(), i);
                VoodooPlayerWidget child = new VoodooPlayerWidget(((LimitedInventoryScreen)(Object)this), x, y, entries.get(i), playerEntry, player.getWorld(), i);
                addDrawableChild(child);
            }
        }
    }

}
