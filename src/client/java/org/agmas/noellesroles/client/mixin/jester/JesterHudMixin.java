package org.agmas.noellesroles.client.mixin.jester;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.gui.JesterTimeRenderer;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 小丑HUD Mixin
 * 在游戏界面上渲染小丑疯魔模式的倒计时
 */
@Mixin(InGameHud.class)
public abstract class JesterHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void jesterPsychoTimeHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        var player = client.player;
        if (player == null) return;
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());

        if (!gameWorldComponent.isRole(player, Noellesroles.JESTER)) {
            return;
        }

        JesterPlayerComponent jesterComponent = JesterPlayerComponent.KEY.get(player);

        if (!jesterComponent.inPsychoMode) {
            return;
        }
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        JesterTimeRenderer.renderHud(renderer, player, context, tickCounter.getTickDelta(true));
    }
}
