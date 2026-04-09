package org.agmas.noellesroles.client.mixin.riotpatrol;

import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Redirect(
        method = "getModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
            ordinal = 0
        )
    )
    private boolean noellesroles$allowRiotForkHandModel(ItemStack stack, Item item) {
        return stack.isOf(item) || item == Items.TRIDENT && stack.isOf(ModItems.RIOT_FORK);
    }

    @Redirect(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
            ordinal = 0
        )
    )
    private boolean noellesroles$allowRiotForkInventoryModel(ItemStack stack, Item item) {
        return stack.isOf(item) || item == Items.TRIDENT && stack.isOf(ModItems.RIOT_FORK);
    }

    @Redirect(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
            ordinal = 2
        )
    )
    private boolean noellesroles$allowRiotForkBuiltinPath(ItemStack stack, Item item) {
        return stack.isOf(item) || item == Items.TRIDENT && stack.isOf(ModItems.RIOT_FORK);
    }
}
