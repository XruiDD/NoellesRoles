package org.agmas.noellesroles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 中立角色（小丑、秃鹫、黑警、病原体）开锁器隐藏 Mixin
 * <p>
 * 当小丑、秃鹫、黑警或病原体拿着开锁器时，其他玩家无法看到他们手中的物品。
 */
@Mixin(HeldItemFeatureRenderer.class)
public class NeutralLockpickHandMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack hideNeutralLockpickMain(LivingEntity instance, Operation<ItemStack> original) {
        ItemStack ret = original.call(instance);
        if (ret.isOf(WatheItems.CROWBAR) && instance instanceof PlayerEntity player) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.JESTER)
                    || gameWorld.isRole(player, Noellesroles.VULTURE)
                    || gameWorld.isRole(player, Noellesroles.CORRUPT_COP)
                    || gameWorld.isRole(player, Noellesroles.PATHOGEN)) {
                return ItemStack.EMPTY;
            }
        }
        if(ret.isOf(ModItems.TIMED_BOMB) && instance instanceof PlayerEntity player) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, Noellesroles.BOMBER)){
                return ItemStack.EMPTY;
            }
        }
        return ret;
    }
}
