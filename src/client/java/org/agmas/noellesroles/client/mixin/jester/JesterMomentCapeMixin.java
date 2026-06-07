package org.agmas.noellesroles.client.mixin.jester;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.SkinTextures;
import org.agmas.noellesroles.client.jester.JesterMomentClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 小丑时刻：受害者视角中，所有玩家的披风同步为小丑的披风。
 * 把 CapeFeatureRenderer 里对被渲染玩家的 getSkinTextures() 重定向到小丑的，
 * 于是披风纹理取小丑的 capeTexture（身体皮肤仍由 JesterMomentSkinMixin 的 getTexture 管 psycho 皮肤）。
 */
@Mixin(value = CapeFeatureRenderer.class, priority = 1500)
public class JesterMomentCapeMixin {

    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    private SkinTextures noellesroles$jesterCape(AbstractClientPlayerEntity instance, Operation<SkinTextures> original) {
        if (JesterMomentClient.isActiveForLocalViewer()) {
            AbstractClientPlayerEntity jester = JesterMomentClient.getActiveJester(instance.getWorld());
            if (jester != null && !instance.getUuid().equals(jester.getUuid())) {
                return jester.getSkinTextures();
            }
        }
        return original.call(instance);
    }
}
