package org.agmas.noellesroles.mixin.bomber;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.item.GrenadeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrenadeItem.class)
public abstract class BomberGrenadeCooldownMixin {

    /**
     * 炸弹客手雷冷却只有一分半
     */
    @Inject(method = "use", at = @At("RETURN"))
    private void modifyGrenadeCooldownForBomber(World world, PlayerEntity user, Hand hand,
                                                 CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (user.isCreative()) return;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(user.getWorld());
        if (gameWorldComponent.isRole(user, Noellesroles.BOMBER)) {
            // 1分30秒 = 90秒 = 1800 ticks
            int customCooldown = GameConstants.getInTicks(1, 30);
            user.getItemCooldownManager().set(WatheItems.GRENADE, customCooldown);
        }
    }
}
