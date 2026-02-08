package org.agmas.noellesroles.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.entity.ThrowingAxeEntity;

@Environment(EnvType.CLIENT)
public class ThrowingAxeEntityRenderer extends EntityRenderer<ThrowingAxeEntity> {
    private final ItemRenderer itemRenderer;
    private final float scale = 1.6F;

    public ThrowingAxeEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrowingAxeEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {

        ItemStack itemStack = entity.getItemStack();
        if (itemStack.isEmpty()) {
            itemStack = new ItemStack(ModItems.THROWING_AXE);
        }

        matrices.push();

        BakedModel bakedModel = this.itemRenderer.getModel(itemStack, entity.getWorld(), null, entity.getId());

        boolean isInGround = entity.isStuckInBlock();

        if (!isInGround) {
            // 飞行旋转动画
            float rotationSpeed = 8.0F;
            float rotation = (entity.getTicksAlive() + tickDelta) * rotationSpeed;

            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation * 0.7F));
        } else {
            // 插在方块上 — 刀刃嵌入表面，刀柄向外突出
            Direction hitDirection = entity.getHitDirection();

            switch (hitDirection) {
                case UP:
                    // 砸入地板：翻转让刀刃朝下嵌入地面，刀柄朝上
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
                    matrices.translate(0.0F, -0.35F, 0.0F);
                    break;
                case DOWN:
                    // 嵌入天花板：刀刃朝上嵌入天花板，刀柄向下
                    matrices.translate(0.0F, -0.35F, 0.0F);
                    break;
                case NORTH:
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                    matrices.translate(0.0F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                    break;
                case SOUTH:
                    matrices.translate(0.0F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                    break;
                case WEST:
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270.0F));
                    matrices.translate(0.0F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                    break;
                case EAST:
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                    matrices.translate(0.0F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                    break;
                default:
                    matrices.translate(0.0F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                    break;
            }
        }

        matrices.scale(this.scale, this.scale, this.scale);

        // 使用全亮度(15728880)让飞斧在黑暗中也清晰可见
        this.itemRenderer.renderItem(
                itemStack, ModelTransformationMode.GROUND, false,
                matrices, vertexConsumers, 15728880,
                OverlayTexture.DEFAULT_UV, bakedModel
        );

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(ThrowingAxeEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
