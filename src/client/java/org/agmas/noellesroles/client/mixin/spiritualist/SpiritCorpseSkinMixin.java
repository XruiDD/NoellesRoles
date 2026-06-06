package org.agmas.noellesroles.client.mixin.spiritualist;

import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.spiritualist.SpiritCameraHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 通灵者灵魂出窍时，覆盖尸体（PlayerBodyEntity）的贴图为 Steve，
 * 与活体玩家的匿名化（SpiritSkinMixin）保持一致——出窍时无法靠尸体皮肤辨认死者身份。
 *
 * 用 SpiritCameraHandler.isActive() 而非 isProjecting()，理由同 SpiritSkinMixin：
 * 避免服务端 sync 下来后、相机关闭前的渲染帧暴露真实皮肤。
 */
@Mixin(PlayerBodyEntityRenderer.class)
public class SpiritCorpseSkinMixin {

    private static final Identifier STEVE_TEXTURE =
            Identifier.ofVanilla("textures/entity/player/wide/steve.png");

    @Inject(
            method = "getTexture(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("HEAD"), cancellable = true)
    private void spiritualist$overrideCorpseTexture(PlayerBodyEntity body,
                                                    CallbackInfoReturnable<Identifier> cir) {
        if (SpiritCameraHandler.isActive()) {
            cir.setReturnValue(STEVE_TEXTURE);
        }
    }
}
