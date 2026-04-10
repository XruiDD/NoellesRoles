package org.agmas.noellesroles.client.mixin.riotpatrol;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.ShieldEntityModel;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltinModelItemRenderer.class)
public class BuiltinModelItemRendererMixin {
    private static final Identifier RIOT_SHIELD_TEXTURE = Identifier.of("noellesroles", "textures/entity/riot_shield.png");

    @Shadow private ShieldEntityModel modelShield;

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
            ordinal = 0
        )
    )
    private boolean noellesroles$allowRiotShieldBuiltinRender(ItemStack stack, Item item) {
        return stack.isOf(item) || item == Items.SHIELD && stack.isOf(ModItems.RIOT_SHIELD);
    }

    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void noellesroles$renderRiotItems(ItemStack stack, ModelTransformationMode renderMode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
        if (stack.isOf(ModItems.RIOT_SHIELD)) {
            matrices.push();
            matrices.scale(1.0F, -1.0F, -1.0F);

            VertexConsumer vertexConsumer = ItemRenderer.getDirectItemGlintConsumer(
                vertexConsumers,
                this.modelShield.getLayer(RIOT_SHIELD_TEXTURE),
                true,
                stack.hasGlint()
            );

            this.modelShield.getHandle().render(matrices, vertexConsumer, light, overlay);
            this.modelShield.getPlate().render(matrices, vertexConsumer, light, overlay);
            matrices.pop();
            ci.cancel();
        }
    }
}
