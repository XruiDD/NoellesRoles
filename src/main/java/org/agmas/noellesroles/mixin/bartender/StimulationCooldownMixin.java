package org.agmas.noellesroles.mixin.bartender;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.server.network.ServerItemCooldownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.mixin.accessor.ServerItemCooldownManagerAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 亢奋期间设置冷却时，只设置 80% 的冷却时长。
 */
@Mixin(ItemCooldownManager.class)
public abstract class StimulationCooldownMixin {

    @ModifyVariable(method = "set", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int reduceCooldownDuringStimulation(int duration) {
        if ((Object) this instanceof ServerItemCooldownManager serverCdm) {
            ServerPlayerEntity player = ((ServerItemCooldownManagerAccessor) serverCdm).getPlayer();
            if (player.hasStatusEffect(ModEffects.STIMULATION)) {
                return (int) (duration * 0.8f);
            }
        }
        return duration;
    }
}
