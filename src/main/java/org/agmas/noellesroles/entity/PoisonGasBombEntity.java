package org.agmas.noellesroles.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;

public class PoisonGasBombEntity extends ThrownItemEntity {
    public PoisonGasBombEntity(EntityType<?> ignored, World world) {
        super(NoellesRolesEntities.POISON_GAS_BOMB_ENTITY, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.POISON_GAS_BOMB;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (this.getWorld() instanceof ServerWorld world) {
            // 生成毒气云实体
            PoisonGasCloudEntity gasCloud = new PoisonGasCloudEntity(NoellesRolesEntities.POISON_GAS_CLOUD_ENTITY, world);
            gasCloud.setPos(this.getX(), this.getY(), this.getZ());
            if (this.getOwner() instanceof ServerPlayerEntity owner) {
                gasCloud.setOwnerUuid(owner.getUuid());
            }
            world.spawnEntity(gasCloud);

            this.discard();
        }
    }
}
