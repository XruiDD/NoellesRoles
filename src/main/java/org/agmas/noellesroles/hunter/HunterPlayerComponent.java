package org.agmas.noellesroles.hunter;

import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;

public class HunterPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<HunterPlayerComponent> KEY = ComponentRegistry.getOrCreate(
        Identifier.of(Noellesroles.MOD_ID, "hunter"), HunterPlayerComponent.class
    );
    private static final Identifier FRACTURE_SPEED_MODIFIER_ID = Identifier.of(Noellesroles.MOD_ID, "fracture_speed");

    public static final int TRAP_ROOT_TICKS = 20 * 3;
    public static final int FRACTURE_LAYER_TICKS = 20 * 60;
    public static final int MAX_FRACTURE_LAYERS = 5;

    private final PlayerEntity player;
    private final List<Integer> fractureTimers = new ArrayList<>();
    private int trappedTicks = 0;

    public HunterPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.fractureTimers.clear();
        this.trappedTicks = 0;
        this.sync();
    }

    public void addFractureLayer() {
        if (this.fractureTimers.size() >= MAX_FRACTURE_LAYERS) {
            return;
        }
        this.fractureTimers.add(FRACTURE_LAYER_TICKS);
        this.sync();
        this.refreshFractureEffect();
    }

    public boolean healOneFractureLayer() {
        if (this.fractureTimers.isEmpty()) {
            return false;
        }
        this.fractureTimers.remove(this.fractureTimers.size() - 1);
        this.refreshFractureEffect();
        this.sync();
        return true;
    }

    public void trap() {
        this.trappedTicks = Math.max(this.trappedTicks, TRAP_ROOT_TICKS);
        this.sync();
    }

    public boolean isTrapped() {
        return this.trappedTicks > 0;
    }

    public int getFractureLayers() {
        return this.fractureTimers.size();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    private void refreshFractureEffect() {
        int layers = this.getFractureLayers();
        if (layers <= 0) {
            this.player.removeStatusEffect(ModEffects.FRACTURE);
            removeFractureSpeedModifier();
            return;
        }

        this.player.addStatusEffect(new StatusEffectInstance(ModEffects.FRACTURE, 10, layers - 1, false, true, true));
        applyFractureSpeedModifier(layers);
    }

    private void applyFractureSpeedModifier(int layers) {
        var attribute = this.player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }

        attribute.removeModifier(FRACTURE_SPEED_MODIFIER_ID);
        int slowingLayers = Math.max(0, layers - 1);
        if (slowingLayers <= 0) {
            return;
        }

        attribute.addPersistentModifier(new EntityAttributeModifier(
            FRACTURE_SPEED_MODIFIER_ID,
            -0.2D * slowingLayers,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private void removeFractureSpeedModifier() {
        var attribute = this.player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(FRACTURE_SPEED_MODIFIER_ID);
        }
    }

    @Override
    public void serverTick() {
        if (this.trappedTicks > 0) {
            boolean wasTrapped = this.trappedTicks > 1;
            this.trappedTicks--;
            this.player.setVelocity(Vec3d.ZERO);
            this.player.velocityModified = true;
            this.player.setSprinting(false);

            PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(this.player);
            if (!stamina.isExhausted()) {
                stamina.setSprintingTicks(stamina.getMaxSprintTime());
                stamina.setExhausted(true);
                stamina.sync();
            }

            if (this.trappedTicks % 20 == 0 || this.trappedTicks == 0) {
                this.sync();
            }
        }

        if (!this.fractureTimers.isEmpty()) {
            boolean changed = false;
            for (int i = this.fractureTimers.size() - 1; i >= 0; i--) {
                int remaining = this.fractureTimers.get(i) - 1;
                if (remaining <= 0) {
                    this.fractureTimers.remove(i);
                    changed = true;
                } else {
                    this.fractureTimers.set(i, remaining);
                }
            }

            this.player.setSprinting(false);
            PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(this.player);
            if (!stamina.isExhausted()) {
                stamina.setSprintingTicks(stamina.getMaxSprintTime());
                stamina.setExhausted(true);
                stamina.sync();
            }

            this.refreshFractureEffect();
            if (changed || this.player.age % 20 == 0) {
                this.sync();
            }
        } else if (this.player.hasStatusEffect(ModEffects.FRACTURE)) {
            this.player.removeStatusEffect(ModEffects.FRACTURE);
            removeFractureSpeedModifier();
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.trappedTicks);
        buf.writeInt(this.fractureTimers.size());
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.trappedTicks = buf.readInt();
        int fractureLayers = buf.readInt();
        this.fractureTimers.clear();
        for (int i = 0; i < fractureLayers; i++) {
            this.fractureTimers.add(FRACTURE_LAYER_TICKS);
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("trappedTicks", this.trappedTicks);
        for (int i = 0; i < this.fractureTimers.size(); i++) {
            tag.putInt("fractureTimer" + i, this.fractureTimers.get(i));
        }
        tag.putInt("fractureCount", this.fractureTimers.size());
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.trappedTicks = tag.getInt("trappedTicks");
        this.fractureTimers.clear();
        int count = tag.getInt("fractureCount");
        for (int i = 0; i < count; i++) {
            this.fractureTimers.add(tag.getInt("fractureTimer" + i));
        }
    }
}
