package org.agmas.noellesroles.client.mixin.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 照搬 Freecam GuiMixin
 * 灵魂出窍时让 HUD（血量、饥饿、经验等）显示真实玩家数据而非 SpiritCamera 数据
 */
@Mixin(InGameHud.class)
public class SpiritGuiMixin {

    @Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true)
    private void spiritualist$useRealPlayer(CallbackInfoReturnable<PlayerEntity> cir) {
        if (SpiritCameraHandler.isActive()) {
            cir.setReturnValue(MinecraftClient.getInstance().player);
        }
    }
}
