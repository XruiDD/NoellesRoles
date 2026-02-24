package org.agmas.noellesroles.mixin.bartender;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.BaseSpiritItem;
import org.agmas.noellesroles.item.IngredientItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 处理玩家在背包中用调制品右键点击基酒的调制逻辑。
 * 玩家先点击调制品（放到光标上），然后右键点击基酒槽位来添加调制品。
 */
@Mixin(ScreenHandler.class)
public abstract class IngredientMixingMixin {

    @Shadow
    @Final
    public DefaultedList<Slot> slots;

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onIngredientMixing(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (slotIndex < 0) return;
        if (actionType != SlotActionType.PICKUP || button != 1) return; // 只处理右键点击

        ItemStack cursorStack = player.currentScreenHandler.getCursorStack();
        if (cursorStack.isEmpty()) return;

        // 光标上必须是调制品
        if (!(cursorStack.getItem() instanceof IngredientItem ingredientItem)) return;

        // 目标槽位必须是基酒
        if (slotIndex >= this.slots.size()) return;
        Slot slot = this.slots.get(slotIndex);
        ItemStack targetStack = slot.getStack();
        if (!targetStack.isOf(ModItems.BASE_SPIRIT)) return;

        // 客户端和服务端都需要 cancel 以防止不同步
        // 实际逻辑只在服务端执行
        if (!player.getWorld().isClient) {
            if (BaseSpiritItem.addIngredient(targetStack, ingredientItem.getIngredientId())) {
                cursorStack.decrement(1);
            }
        }
        ci.cancel();
    }
}
