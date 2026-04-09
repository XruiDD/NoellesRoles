package org.agmas.noellesroles;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.entity.RoleMineEntity;
import org.agmas.noellesroles.entity.PoisonGasBombEntity;
import org.agmas.noellesroles.entity.PoisonGasCloudEntity;
import org.agmas.noellesroles.entity.ThrowingAxeEntity;
import org.agmas.noellesroles.entity.HunterTrapEntity;

public class NoellesRolesEntities {
    public static final EntityType<RoleMineEntity> ROLE_MINE_ENTITY_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "cube"),
            EntityType.Builder.create(RoleMineEntity::new, SpawnGroup.MISC).dimensions(0.75f, 0.75f).build("cube")
    );

    public static final EntityType<PoisonGasBombEntity> POISON_GAS_BOMB_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "poison_gas_bomb"),
            EntityType.Builder.create(PoisonGasBombEntity::new, SpawnGroup.MISC).dimensions(0.2f, 0.2f).build("poison_gas_bomb")
    );

    public static final EntityType<PoisonGasCloudEntity> POISON_GAS_CLOUD_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "poison_gas_cloud"),
            EntityType.Builder.create(PoisonGasCloudEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f).build("poison_gas_cloud")
    );

    public static final EntityType<ThrowingAxeEntity> THROWING_AXE_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "throwing_axe"),
            EntityType.Builder.<ThrowingAxeEntity>create(ThrowingAxeEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f).build("throwing_axe")
    );

    public static final EntityType<HunterTrapEntity> HUNTER_TRAP_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "hunter_trap"),
            EntityType.Builder.<HunterTrapEntity>create(HunterTrapEntity::new, SpawnGroup.MISC).dimensions(0.6f, 0.1f).build("hunter_trap")
    );

    public static void init() {}
}
