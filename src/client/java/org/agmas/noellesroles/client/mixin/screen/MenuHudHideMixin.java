package org.agmas.noellesroles.client.mixin.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.agmas.noellesroles.client.screen.AssassinScreen;
import org.agmas.noellesroles.client.screen.CriminalReasonerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MenuHudHideMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideHudForRoleMenus(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof AssassinScreen
                || MinecraftClient.getInstance().currentScreen instanceof CriminalReasonerScreen) {
            ci.cancel();
        }
    }
}
