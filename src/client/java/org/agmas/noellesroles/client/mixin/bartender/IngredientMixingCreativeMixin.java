package org.agmas.noellesroles.client.mixin.bartender;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.BaseSpiritItem;
import org.agmas.noellesroles.item.IngredientItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 创造模式下的调制逻辑。
 * CreativeInventoryScreen 覆盖了 onMouseClick，导致 ScreenHandler 的 mixin 不生效，
 * 因此需要在客户端侧单独处理，并通过 CreativeInventoryActionC2SPacket 同步到服务端。
 */
@Mixin(CreativeInventoryScreen.class)
public abstract class IngredientMixingCreativeMixin extends HandledScreen {

    private IngredientMixingCreativeMixin() {
        super(null, null, null);
    }

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
    private void onCreativeIngredientMixing(@Nullable Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (button != 1 || actionType != SlotActionType.PICKUP) return;
        if (slot == null) return;

        ItemStack cursorStack = this.handler.getCursorStack();
        if (cursorStack.isEmpty()) return;
        if (!(cursorStack.getItem() instanceof IngredientItem ingredientItem)) return;

        ItemStack targetStack = slot.getStack();
        if (!targetStack.isOf(ModItems.BASE_SPIRIT)) return;

        if (BaseSpiritItem.addIngredient(targetStack, ingredientItem.getIngredientId())) {
            // 通过创造模式数据包同步修改后的基酒到服务端
            if (this.client != null && this.client.player != null) {
                this.client.player.networkHandler.sendPacket(
                        new CreativeInventoryActionC2SPacket(slot.id, targetStack)
                );
            }
        }
        ci.cancel();
    }
}
