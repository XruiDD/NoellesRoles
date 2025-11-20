package org.agmas.noellesroles.mixin.host;

import dev.doctor4t.trainmurdermystery.block.TrainDoorBlock;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.RoleHelpers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TrainDoorBlock.class)
public abstract class MasterKeyTrainDoorMixin {

    @Redirect(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
    private boolean host(ItemStack instance, Item item) {
        return instance.isOf(TMMItems.LOCKPICK) || instance.isOf(ModItems.MASTER_KEY);
    }

}
