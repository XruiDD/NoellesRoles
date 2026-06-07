package org.agmas.noellesroles.client.mixin.jester;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.PlayerCosmeticsComponent;
import dev.doctor4t.wathe.client.skin.PlayerSkinTextureManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.jester.JesterMomentClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 小丑时刻：所有玩家渲染成"活跃小丑"的疯魔皮肤。
 */
@Mixin(value = PlayerEntityRenderer.class, priority = 1500)
public class JesterMomentSkinMixin {

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("HEAD"), cancellable = true)
    private void noellesroles$jesterMomentSkin(AbstractClientPlayerEntity rendered,
                                               CallbackInfoReturnable<Identifier> cir) {
        if (!JesterMomentClient.isActiveForLocalViewer()) return;
        AbstractClientPlayerEntity jester = JesterMomentClient.getActiveJester(rendered.getWorld());
        if (jester == null) return;
        cir.setReturnValue(resolveJesterPsychoTexture(jester, rendered));
    }

    @Inject(method = "getArmPose", at = @At("TAIL"), cancellable = true)
    private static void noellesroles$jesterMomentArmPose(AbstractClientPlayerEntity player, Hand hand,
                                                         CallbackInfoReturnable<BipedEntityModel.ArmPose> cir) {
        // 小丑时刻：所有人主手摆出球棒握姿（与视觉球棒一致；受害者并未真持球棒，故 wathe 原逻辑不触发，需在此补）
        if (JesterMomentClient.isActiveForLocalViewer() && hand == Hand.MAIN_HAND) {
            cir.setReturnValue(BipedEntityModel.ArmPose.CROSSBOW_CHARGE);
        }
    }

    /**
     * 取小丑的疯魔 cosmetic 应用到任意被渲染玩家。
     * 设计取舍（有意，不同于 wathe 的自渲染逻辑）：用户要求"所有人皮肤都必须是小丑的"，
     * 故无条件采用小丑 cosmetic（不做 model 匹配守卫）以保证全场视觉一致；小丑 cosmetic 模型
     * 与被渲染者模型不同的少数情况下仅臂宽(3/4px)轻微错位，已接受。
     * 兜底（小丑未装备 cosmetic）才回退内置 psycho，并按【被渲染者自身模型】选 thin/wide，
     * 使内置贴图与该玩家臂宽匹配、无错位（内置 psycho 非小丑专属，不损一致性）。
     */
    private static Identifier resolveJesterPsychoTexture(AbstractClientPlayerEntity jester,
                                                         AbstractClientPlayerEntity rendered) {
        PlayerCosmeticsComponent.PlayerSkinEntry entry =
                PlayerCosmeticsComponent.KEY.get(jester).getPlayerSkin("psycho");
        if (entry != null) {
            Identifier texId = PlayerSkinTextureManager.getInstance().getTextureId(entry.textureUrl());
            if (texId != null) return texId;
            PlayerSkinTextureManager.getInstance().ensureLoaded(entry.textureUrl());
        }
        boolean slim = rendered.getSkinTextures().model() == SkinTextures.Model.SLIM;
        return Wathe.id("textures/entity/psycho" + (slim ? "_thin" : "") + ".png");
    }
}
