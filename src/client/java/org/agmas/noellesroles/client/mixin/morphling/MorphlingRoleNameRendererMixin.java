package org.agmas.noellesroles.client.mixin.morphling;

import dev.doctor4t.trainmurdermystery.client.gui.RoleNameRenderer;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(RoleNameRenderer.class)
public abstract class MorphlingRoleNameRendererMixin {

    @Redirect(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private static Text b(PlayerEntity instance) {
        if ((MorphlingPlayerComponent.KEY.get(instance)).getMorphTicks() > 0) {
            if (instance.getWorld().getPlayerByUuid(MorphlingPlayerComponent.KEY.get(instance).disguise) != null) {
                return instance.getWorld().getPlayerByUuid((MorphlingPlayerComponent.KEY.get(instance)).disguise).getDisplayName();
            } else {
                Log.info(LogCategory.GENERAL, "Morphling disguise is null!!!");
            }
            if (MorphlingPlayerComponent.KEY.get(instance).disguise.equals(MinecraftClient.getInstance().player.getUuid())) {
                return MinecraftClient.getInstance().player.getDisplayName();
            }
        }
        return instance.getDisplayName();
    }

}
