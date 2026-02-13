package org.agmas.noellesroles.mixin.silencer;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
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

@Mixin(Item.class)
public class SilencedEatingMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void noellesroles$preventSilencedEating(
            World world, PlayerEntity user, Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack.get(DataComponentTypes.FOOD) != null
                && SilencedPlayerComponent.isPlayerSilenced(user)) {
            if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(
                        Text.translatable("tip.silenced.cannot_eat")
                                .formatted(Formatting.RED),
                        true
                );
            }
            cir.setReturnValue(TypedActionResult.fail(stack));
        }
    }
}
