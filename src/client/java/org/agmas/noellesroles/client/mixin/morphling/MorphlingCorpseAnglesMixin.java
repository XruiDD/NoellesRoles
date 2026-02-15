package org.agmas.noellesroles.client.mixin.morphling;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MorphlingCorpseAnglesMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow protected M model;

    /**
     * 在 model.setAngles() 调用之后，移除尸体模式下的头部跟随视角和自然空闲手臂摆动。
     * 保留其余所有动画（行走摆动、持物姿势、挥手、潜行等）。
     */
    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER))
    void noellesroles$resetCorpseAngles(T entity, float f, float g, MatrixStack matrixStack,
            VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerEntity)) return;
        MorphlingPlayerComponent comp = MorphlingPlayerComponent.KEY.get(entity);
        if (comp.corpseMode && this.model instanceof PlayerEntityModel<?> playerModel) {
            // 头部固定不动（不跟随视角）
            playerModel.head.pitch = 0;
            playerModel.head.yaw = 0;
            playerModel.hat.copyTransform(playerModel.head);

            // 撤销 CrossbowPosing.swingArm 添加的自然空闲摆动
            // 公式: arm.roll += sigma * (cos(age * 0.09) * 0.05 + 0.05)
            //        arm.pitch += sigma * (sin(age * 0.067) * 0.05)
            float ageInTicks = entity.age + g;
            float swingRollBase = MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
            float swingPitchBase = MathHelper.sin(ageInTicks * 0.067F) * 0.05F;

            // 右臂 sigma=1.0F
            playerModel.rightArm.roll -= swingRollBase;
            playerModel.rightArm.pitch -= swingPitchBase;
            // 左臂 sigma=-1.0F
            playerModel.leftArm.roll -= -swingRollBase;
            playerModel.leftArm.pitch -= -swingPitchBase;

            // 同步装饰层
            playerModel.leftSleeve.copyTransform(playerModel.leftArm);
            playerModel.rightSleeve.copyTransform(playerModel.rightArm);
        }
    }
}
