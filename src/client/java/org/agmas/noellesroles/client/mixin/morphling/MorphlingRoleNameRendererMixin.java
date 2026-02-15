package org.agmas.noellesroles.client.mixin.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


@Mixin(RoleNameRenderer.class)
public abstract class MorphlingRoleNameRendererMixin {

    @WrapOperation(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private static Text b(PlayerEntity instance, Operation<Text> original) {

        if (WatheClient.moodComponent != null) {
            if ((ConfigWorldComponent.KEY.get(instance.getWorld())).insaneSeesMorphs && WatheClient.moodComponent.isLowerThanDepressed() && NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.get(instance.getUuid()) != null) {
                return Text.literal("??!?!").formatted(Formatting.OBFUSCATED);
            }
        }
        if (instance.isInvisible()) {
            return Text.literal("");
        }
        MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(instance);
        // 尸体模式下隐藏名称标签（独立于换皮变形）
        if (morphComp.corpseMode) {
            return Text.literal("");
        }
        if (morphComp.getMorphTicks() > 0) {
            PlayerEntity disguisePlayer = instance.getWorld().getPlayerByUuid(morphComp.disguise);
            if (disguisePlayer != null) {
                return disguisePlayer.getDisplayName();
            }
            // 目标不在世界中（已死亡），回退到缓存获取名字
            PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(morphComp.disguise);
            if (cachedEntry != null) {
                return cachedEntry.getDisplayName() != null
                        ? cachedEntry.getDisplayName()
                        : Text.literal(cachedEntry.getProfile().getName());
            }
            if (morphComp.disguise.equals(MinecraftClient.getInstance().player.getUuid())) {
                return MinecraftClient.getInstance().player.getDisplayName();
            }
        }
        return original.call(instance);
    }

}
