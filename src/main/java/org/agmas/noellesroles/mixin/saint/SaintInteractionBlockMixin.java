package org.agmas.noellesroles.mixin.saint;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.saint.SaintPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class SaintInteractionBlockMixin {
    @Shadow @Final protected ServerPlayerEntity player;

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockKarmaInteractItem(CallbackInfoReturnable<ActionResult> cir) {
        if (!SaintPlayerComponent.KEY.get(this.player).isKarmaLocked()) {
            return;
        }
        this.player.sendMessage(Text.translatable("tip.saint.karma_locked", Math.max(1, SaintPlayerComponent.KEY.get(this.player).getKarmaLockTicks() / 20)).formatted(Formatting.RED), true);
        cir.setReturnValue(ActionResult.FAIL);
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockKarmaInteractBlock(CallbackInfoReturnable<ActionResult> cir) {
        if (!SaintPlayerComponent.KEY.get(this.player).isKarmaLocked()) {
            return;
        }
        this.player.sendMessage(Text.translatable("tip.saint.karma_locked", Math.max(1, SaintPlayerComponent.KEY.get(this.player).getKarmaLockTicks() / 20)).formatted(Formatting.RED), true);
        cir.setReturnValue(ActionResult.FAIL);
    }

}
