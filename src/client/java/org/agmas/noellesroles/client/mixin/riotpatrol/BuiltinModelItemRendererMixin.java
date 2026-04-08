package org.agmas.noellesroles.client.mixin.riotpatrol;

import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BuiltinModelItemRenderer.class)
public class BuiltinModelItemRendererMixin {

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
            ordinal = 0
        )
    )
    private boolean noellesroles$allowRiotShieldBuiltinRender(ItemStack stack, Item item) {
        return stack.isOf(item) || item == Items.SHIELD && stack.isOf(ModItems.RIOT_SHIELD);
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
            ordinal = 1
        )
    )
    private boolean noellesroles$allowRiotForkBuiltinRender(ItemStack stack, Item item) {
        return stack.isOf(item) || item == Items.TRIDENT && stack.isOf(ModItems.RIOT_FORK);
    }
}
