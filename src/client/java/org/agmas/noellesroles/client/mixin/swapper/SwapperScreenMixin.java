package org.agmas.noellesroles.client.mixin.swapper;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.SwapperPlayerWidget;
import org.agmas.noellesroles.client.widget.PlayerSelectWidget;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Mixin(LimitedInventoryScreen.class)
public abstract class SwapperScreenMixin extends LimitedHandledScreen<PlayerScreenHandler>{
    @Shadow @Final public ClientPlayerEntity player;

    public SwapperScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }


    @Inject(method = "render", at = @At("HEAD"))
    void a(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player,Noellesroles.SWAPPER)) {
            int y = (height- 32) / 2;
            int x = width / 2;
            if (SwapperPlayerWidget.playerChoiceOne == null) {
                Text name = Text.translatable("hud.swapper.first_player_selection");
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, name, x - (MinecraftClient.getInstance().textRenderer.getWidth(name)/2), y + 35, Color.CYAN.getRGB());
            } else {
                Text name = Text.translatable("hud.swapper.second_player_selection");
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, name, x - (MinecraftClient.getInstance().textRenderer.getWidth(name)/2), y + 35, Color.RED.getRGB());
            }
        }
    }
    @Inject(method = "init", at = @At("HEAD"))
    void b(CallbackInfo ci) {
        SwapperPlayerWidget.playerChoiceOne = null;
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player,Noellesroles.SWAPPER)) {
            List<UUID> lives = gameWorldComponent.getAllAlivePlayers();
            List<UUID> entries = new ArrayList<>(WatheClient.PLAYER_ENTRIES_CACHE.keySet());
            entries.removeIf(uuid -> !lives.contains(uuid));
            entries.removeIf(uuid -> {
                var targetPlayer = player.getWorld().getPlayerByUuid(uuid);
                return targetPlayer != null && SwallowedPlayerComponent.isPlayerSwallowed(targetPlayer);
            });

            for(int i = 0; i < entries.size(); ++i) {
                int x = PlayerSelectWidget.calculateGridX(width, entries.size(), i);
                int y = PlayerSelectWidget.calculateGridY(height, entries.size(), i);
                SwapperPlayerWidget child = new SwapperPlayerWidget(((LimitedInventoryScreen)(Object)this), x, y, entries.get(i), i);
                addDrawableChild(child);
            }
        }
    }

}
