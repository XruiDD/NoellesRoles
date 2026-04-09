package org.agmas.noellesroles.mixin.saint;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.saint.SaintPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class SaintItemUseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockKarmaUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        SaintPlayerComponent saintComponent = SaintPlayerComponent.KEY.get(serverPlayer);
        if (!saintComponent.isKarmaLocked()) {
            return;
        }
        serverPlayer.sendMessage(Text.translatable("tip.saint.karma_locked", Math.max(1, saintComponent.getKarmaLockTicks() / 20)).formatted(Formatting.RED), true);
        cir.setReturnValue(TypedActionResult.fail(user.getStackInHand(hand)));
    }

    @Inject(method = "useOnEntity", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockKarmaUseOnEntity(ItemStack stack, PlayerEntity user, net.minecraft.entity.LivingEntity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        SaintPlayerComponent saintComponent = SaintPlayerComponent.KEY.get(serverPlayer);
        if (!saintComponent.isKarmaLocked()) {
            return;
        }
        serverPlayer.sendMessage(Text.translatable("tip.saint.karma_locked", Math.max(1, saintComponent.getKarmaLockTicks() / 20)).formatted(Formatting.RED), true);
        cir.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockKarmaUseOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        SaintPlayerComponent saintComponent = SaintPlayerComponent.KEY.get(serverPlayer);
        if (!saintComponent.isKarmaLocked()) {
            return;
        }
        serverPlayer.sendMessage(Text.translatable("tip.saint.karma_locked", Math.max(1, saintComponent.getKarmaLockTicks() / 20)).formatted(Formatting.RED), true);
        cir.setReturnValue(ActionResult.FAIL);
    }
}
