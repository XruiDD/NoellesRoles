package org.agmas.noellesroles.client.mixin.bartender;

import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 酒保防御药剂视觉效果 Mixin
 * <p>
 * 此 Mixin 用于实现酒保的防御药剂可以被酒保自己看到的功能。
 * 当酒保在饮料中放入防御药剂时，酒保玩家会看到绿色粒子效果（高兴村民粒子），
 * 以区分酒保的防御药剂和杀手的毒药（紫色粒子）。
 */
@Mixin(BeveragePlateBlockEntity.class)
public class DefenseVialViewMixin {

    /**
     * 在饮料盘的客户端 tick 中注入，用于显示酒保防御药剂的特殊粒子效果
     * <p>
     * 触发条件：
     * 1. 饮料盘上的饮料已被下毒
     * 2. 下毒者是酒保角色
     * 3. 当前玩家也是酒保角色（只有酒保能看到自己放的防御药剂）
     * <p>
     * 效果：显示绿色高兴村民粒子，并取消原版的紫色毒药粒子效果
     */
    @Inject(method = "clientTick", at = @At("HEAD"), order = 1001, cancellable = true)
    private static void view(World world, BlockPos pos, BlockState state, BlockEntity blockEntity, CallbackInfo ci) {
        if (blockEntity instanceof BeveragePlateBlockEntity tray) {
            if (tray.getPoisoner() != null) {
                if (MinecraftClient.getInstance().player != null && GameWorldComponent.KEY.get(world).isRole(UUID.fromString(tray.getPoisoner()), Noellesroles.BARTENDER) && GameWorldComponent.KEY.get(world).isRole(MinecraftClient.getInstance().player, Noellesroles.BARTENDER)) {
                    world.addParticle(ParticleTypes.HAPPY_VILLAGER,  ((float) pos.getX() + 0.5F),  pos.getY(), ((float) pos.getZ() + 0.5F),  0.0F,  0.15F,  0.0F);
                    ci.cancel();
                }
            }
        }
    }
}
