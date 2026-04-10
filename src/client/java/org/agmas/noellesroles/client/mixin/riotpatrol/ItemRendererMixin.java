package org.agmas.noellesroles.client.mixin.riotpatrol;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Redirect(
        method = "getModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
            ordinal = 0
        )
    )
    private boolean noellesroles$allowRiotForkHandModel(ItemStack stack, Item item) {
        return isRiotForkOrOriginal(stack, item);
    }

    @Redirect(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
            ordinal = 0
        )
    )
    private boolean noellesroles$allowRiotForkInventoryModel(ItemStack stack, Item item) {
        return isRiotForkOrOriginal(stack, item);
    }

    @Redirect(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
            ordinal = 2
        )
    )
    private boolean noellesroles$allowRiotForkBuiltinPath(ItemStack stack, Item item) {
        return isRiotForkOrOriginal(stack, item);
    }

    private static boolean isRiotForkOrOriginal(ItemStack stack, Item item) {
        return stack.isOf(item) || (item == Items.TRIDENT && stack.isOf(ModItems.RIOT_FORK));
    }

    @ModifyVariable(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private BakedModel noellesroles$swapRiotForkInHandModel(
        BakedModel model,
        ItemStack stack,
        ModelTransformationMode renderMode,
        boolean leftHanded
    ) {
        if (stack.isOf(ModItems.RIOT_FORK) && noellesroles$isHandRenderMode(renderMode)) {
            return ((FabricBakedModelManager) MinecraftClient.getInstance().getBakedModelManager()).getModel(NoellesrolesClient.RIOT_FORK_IN_HAND_MODEL_ID);
        }

        return model;
    }

    private static boolean noellesroles$isHandRenderMode(ModelTransformationMode renderMode) {
        return renderMode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND
            || renderMode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
            || renderMode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
            || renderMode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
    }
}
