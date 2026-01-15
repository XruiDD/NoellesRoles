package org.agmas.noellesroles.client.mixin.bartender;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 酒保防御药剂其他人不可见 Mixin
 */
@Mixin(HeldItemFeatureRenderer.class)
public class DefenseVialHandMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack view(LivingEntity instance, Operation<ItemStack> original) {
        ItemStack ret = original.call(instance);
        if (!GameFunctions.isPlayerAliveAndSurvival(MinecraftClient.getInstance().player)) return ret;

        if (ret.isOf(ModItems.DEFENSE_VIAL)) {
            ret = ItemStack.EMPTY;
        }
        if (ret.isOf(ModItems.NEUTRAL_MASTER_KEY)) {
            ret = ItemStack.EMPTY;
        }
        return ret;
    }
}
