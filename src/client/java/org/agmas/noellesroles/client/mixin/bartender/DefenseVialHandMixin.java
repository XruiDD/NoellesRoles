package org.agmas.noellesroles.client.mixin.bartender;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 隐藏特定道具对其他玩家不可见 Mixin
 * 包括：防御药剂、中立钥匙、万能钥匙、解毒剂、铁人药剂、便签、书籍
 */
@Mixin(HeldItemFeatureRenderer.class)
public class DefenseVialHandMixin {

    /**
     * 检查物品是否应该被隐藏
     */
    private static boolean shouldHideItem(ItemStack stack) {
        // 原有隐藏：防御药剂、中立钥匙
        if (stack.isOf(ModItems.DEFENSE_VIAL)) return true;
        if (stack.isOf(ModItems.NEUTRAL_MASTER_KEY)) return true;
        // 好人角色道具隐藏：解毒剂（毒理学家）
        if (stack.isOf(ModItems.ANTIDOTE)) return true;
        // 好人角色道具隐藏：铁人药剂（教授）
        if (stack.isOf(ModItems.IRON_MAN_VIAL)) return true;
        // 好人角色道具隐藏：书籍（乘务员）
        if (stack.isOf(Items.WRITTEN_BOOK)) return true;
        return false;
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack hideMainHand(LivingEntity instance, Operation<ItemStack> original) {
        ItemStack ret = original.call(instance);
        if (!GameFunctions.isPlayerAliveAndSurvival(MinecraftClient.getInstance().player)) return ret;
        if (shouldHideItem(ret)) return ItemStack.EMPTY;
        return ret;
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getOffHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack hideOffHand(LivingEntity instance, Operation<ItemStack> original) {
        ItemStack ret = original.call(instance);
        if (!GameFunctions.isPlayerAliveAndSurvival(MinecraftClient.getInstance().player)) return ret;
        if (shouldHideItem(ret)) return ItemStack.EMPTY;
        return ret;
    }
}
