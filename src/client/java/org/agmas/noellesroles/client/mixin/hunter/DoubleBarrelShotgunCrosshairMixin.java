package org.agmas.noellesroles.client.mixin.hunter;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.DoubleBarrelShotgunItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CrosshairRenderer.class)
public class DoubleBarrelShotgunCrosshairMixin {

    @ModifyExpressionValue(
            method = "renderCrosshair",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/doctor4t/wathe/client/gui/CrosshairRenderer;CROSSHAIR:Lnet/minecraft/util/Identifier;"
            )
    )
    private static Identifier noellesroles$showShotgunTargetCrosshair(
            Identifier original,
            MinecraftClient client,
            ClientPlayerEntity player,
            DrawContext context,
            RenderTickCounter tickCounter
    ) {
        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(ModItems.DOUBLE_BARREL_SHOTGUN)
                || player.getItemCooldownManager().isCoolingDown(stack.getItem())
                || DoubleBarrelShotgunItem.getLoadedShells(stack) <= 0
                || DoubleBarrelShotgunItem.findTarget(player) == null) {
            return original;
        }

        return Identifier.of("wathe", "hud/crosshair_target");
    }
}
