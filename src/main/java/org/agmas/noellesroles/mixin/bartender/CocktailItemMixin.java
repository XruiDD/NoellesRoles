package org.agmas.noellesroles.mixin.bartender;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.CocktailItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


//吃东西发光
@Mixin(CocktailItem.class)
public class CocktailItemMixin {

    @Inject(method = "finishUsing", at = @At("HEAD"))
    public void bartenderVision(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {

        ((BartenderPlayerComponent)BartenderPlayerComponent.KEY.get(user)).startGlow();
    }
}
