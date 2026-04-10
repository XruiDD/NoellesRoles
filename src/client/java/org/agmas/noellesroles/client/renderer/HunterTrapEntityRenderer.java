package org.agmas.noellesroles.client.renderer;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.entity.HunterTrapEntity;

public class HunterTrapEntityRenderer extends EntityRenderer<HunterTrapEntity> {
    private final ItemRenderer itemRenderer;

    public HunterTrapEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(HunterTrapEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0F, 0.18F, 0.0F);
        matrices.multiply(this.dispatcher.getRotation());
        matrices.scale(0.85F, 0.85F, 0.85F);

        ItemStack itemStack = entity.asPickupStack();
        BakedModel bakedModel = this.itemRenderer.getModel(itemStack, entity.getWorld(), null, entity.getId());
        this.itemRenderer.renderItem(itemStack, ModelTransformationMode.FIXED, false, matrices, vertexConsumers, light, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, bakedModel);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public boolean shouldRender(HunterTrapEntity entity, Frustum frustum, double x, double y, double z) {
        var player = MinecraftClient.getInstance().player;
        return player != null && entity.canBeSeenBy(player);
    }

    @Override
    public Identifier getTexture(HunterTrapEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}
