package org.agmas.noellesroles.mermaid;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheAttributes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 鲛人角色组件（纯服务端）
 * - 在水中获得海豚的恩惠、夜视（无粒子效果）
 * - 水中奔跑体力消耗减少75%（属性修改器增加300%体力上限）
 * - 水中氧气消耗减少75%（通过 MaxAir 事件将 maxAir 乘以4倍实现）
 */
public class MermaidPlayerComponent implements Component, ServerTickingComponent {
    public static final ComponentKey<MermaidPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "mermaid"), MermaidPlayerComponent.class);

    private static final Identifier MERMAID_SPRINT_MODIFIER_ID = Identifier.of(Noellesroles.MOD_ID, "mermaid_sprint");
    private static final int EFFECT_DURATION = 400;
    private static final int CHECK_INTERVAL = 20;

    private final PlayerEntity player;
    private boolean inWater = false;
    private int tickCounter = 0;

    public MermaidPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    /**
     * 由 MaxAir 事件调用，判断是否应该增加氧气上限
     */
    public boolean shouldModifyAir() {
        return inWater;
    }

    @Override
    public void serverTick() {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        GameWorldComponent gameComp = GameWorldComponent.KEY.get(player.getWorld());
        boolean isMermaidAndAlive = gameComp.isRole(player, Noellesroles.MERMAID)
                && GameFunctions.isPlayerPlayingAndAlive(player);

        if (!isMermaidAndAlive) {
            if (inWater) {
                cleanupEffects(serverPlayer);
            }
            return;
        }

        boolean nowInWater = player.isTouchingWater();

        if (nowInWater) {
            serverPlayer.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.DOLPHINS_GRACE, EFFECT_DURATION, 0, false, false, true));
            serverPlayer.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, EFFECT_DURATION, 0, false, false, true));
            applyStaminaModifier(serverPlayer);
            inWater = true;
        } else if (inWater) {
            cleanupEffects(serverPlayer);
        }
    }

    private void applyStaminaModifier(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (attr != null && !attr.hasModifier(MERMAID_SPRINT_MODIFIER_ID)) {
            attr.addTemporaryModifier(new EntityAttributeModifier(
                    MERMAID_SPRINT_MODIFIER_ID, 3.0,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private void removeStaminaModifier(ServerPlayerEntity player) {
        EntityAttributeInstance attr = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (attr != null && attr.hasModifier(MERMAID_SPRINT_MODIFIER_ID)) {
            attr.removeModifier(MERMAID_SPRINT_MODIFIER_ID);
        }
    }

    private void cleanupEffects(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        removeStaminaModifier(player);
        inWater = false;
    }

    public void reset() {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            cleanupEffects(serverPlayer);
        }
        inWater = false;
        tickCounter = 0;
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
    }
}
