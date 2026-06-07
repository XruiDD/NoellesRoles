package org.agmas.noellesroles.client.mixin.jester;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.client.jester.JesterMomentClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 小丑时刻：所有玩家在他人视角中"手持球棒"（纯视觉）。
 */
@Mixin(value = HeldItemFeatureRenderer.class, priority = 1500)
public class JesterMomentHeldItemMixin {

    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack noellesroles$jesterMomentBat(LivingEntity instance, Operation<ItemStack> original) {
        if (JesterMomentClient.isActiveForLocalViewer() && instance instanceof PlayerEntity) {
            AbstractClientPlayerEntity jester = JesterMomentClient.getActiveJester(instance.getWorld());
            if (jester != null) {
                // 小丑本人：照常渲染其真实球棒（已带 SKIN 皮肤组件）
                if (instance.getUuid().equals(jester.getUuid())) {
                    return original.call(instance);
                }
                // 其他玩家：复制小丑真实球棒（连同同步过来的 SKIN 组件），显示小丑的球棒皮肤
                ItemStack jesterBat = jester.getMainHandStack();
                if (jesterBat.isOf(WatheItems.BAT)) {
                    return jesterBat.copy();
                }
            }
            // 兜底：小丑当前未持球棒/未找到 → 裸球棒
            return new ItemStack(WatheItems.BAT);
        }
        return original.call(instance);
    }
}
