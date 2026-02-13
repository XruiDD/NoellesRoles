package org.agmas.noellesroles.mixin.silencer;

import dev.doctor4t.wathe.item.CocktailItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.silencer.SilencedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CocktailItem.class)
public class SilencedDrinkingMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void noellesroles$preventSilencedDrinking(
            World world, PlayerEntity user, Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (SilencedPlayerComponent.isPlayerSilenced(user)) {
            if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(
                        Text.translatable("tip.silenced.cannot_drink")
                                .formatted(Formatting.RED),
                        true
                );
            }
            cir.setReturnValue(TypedActionResult.fail(user.getStackInHand(hand)));
        }
    }
}
