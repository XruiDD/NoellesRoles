package org.agmas.noellesroles.mixin.scavenger;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.item.KnifeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KnifeItem.class)
public abstract class ScavengerKnifeSoundMixin {

    /**
     * 清道夫角色举刀时不播放准备音效
     */
    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"))
    private void scavengerSilentKnifePrepare(PlayerEntity user, SoundEvent sound, float volume, float pitch) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(user.getWorld());
        if (gameWorldComponent.isRole(user, Noellesroles.SCAVENGER)) {
            return;
        }
        user.playSound(sound, volume, pitch);
    }
}
