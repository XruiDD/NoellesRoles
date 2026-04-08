package org.agmas.noellesroles.riotpatrol;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.item.RiotShieldItem;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class RiotPatrolPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<RiotPatrolPlayerComponent> KEY = ComponentRegistry.getOrCreate(
        Identifier.of(Noellesroles.MOD_ID, "riot_patrol"), RiotPatrolPlayerComponent.class
    );

    private final PlayerEntity player;
    private boolean shieldActive = false;
    private float lockedYaw = 0.0F;
    private float lockedPitch = 0.0F;
    private int rootedTicks = 0;
    private double rootedX = 0.0;
    private double rootedY = 0.0;
    private double rootedZ = 0.0;

    public RiotPatrolPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.shieldActive = false;
        this.rootedTicks = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.shieldActive);
        buf.writeFloat(this.lockedYaw);
        buf.writeFloat(this.lockedPitch);
        buf.writeInt(this.rootedTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.shieldActive = buf.readBoolean();
        this.lockedYaw = buf.readFloat();
        this.lockedPitch = buf.readFloat();
        this.rootedTicks = buf.readInt();
    }

    public void raiseShield() {
        if (this.shieldActive) {
            return;
        }
        this.shieldActive = true;
        this.lockedYaw = this.player.getYaw();
        this.lockedPitch = this.player.getPitch();
        this.sync();
    }

    public void lowerShield(boolean applyCooldown) {
        if (!this.shieldActive) {
            return;
        }
        this.shieldActive = false;
        if (applyCooldown) {
            this.player.getItemCooldownManager().set(ModItems.RIOT_SHIELD, RiotShieldItem.SHIELD_COOLDOWN_TICKS);
        }
        this.sync();
    }

    public boolean isShieldActive() {
        return this.shieldActive;
    }

    public float getLockedYaw() {
        return this.lockedYaw;
    }

    public float getLockedPitch() {
        return this.lockedPitch;
    }

    public void rootAtCurrentPosition(int ticks) {
        this.rootedTicks = Math.max(this.rootedTicks, ticks);
        this.rootedX = this.player.getX();
        this.rootedY = this.player.getY();
        this.rootedZ = this.player.getZ();
        this.sync();
    }

    public boolean isRooted() {
        return this.rootedTicks > 0;
    }

    public boolean blocksAttacker(PlayerEntity attacker) {
        if (!this.shieldActive || attacker == null) {
            return false;
        }

        Vec3d look = this.player.getRotationVec(1.0F);
        Vec3d toAttacker = attacker.getPos().subtract(this.player.getPos());
        Vec3d horizontalLook = new Vec3d(look.x, 0.0, look.z);
        Vec3d horizontalAttacker = new Vec3d(toAttacker.x, 0.0, toAttacker.z);
        if (horizontalLook.lengthSquared() < 1.0E-6 || horizontalAttacker.lengthSquared() < 1.0E-6) {
            return true;
        }
        return horizontalLook.normalize().dotProduct(horizontalAttacker.normalize()) > 0.35;
    }

    @Override
    public void serverTick() {
        if (this.shieldActive) {
            if (!this.player.isUsingItem() || !this.player.getActiveItem().isOf(ModItems.RIOT_SHIELD)) {
                this.lowerShield(false);
            } else {
                this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 3, false, false, false));
                if (this.player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.setYaw(this.lockedYaw);
                    serverPlayer.setPitch(this.lockedPitch);
                }
            }
        }

        if (this.rootedTicks <= 0) {
            return;
        }

        this.rootedTicks--;
        if (this.player instanceof ServerPlayerEntity serverPlayer && this.player.getWorld() instanceof ServerWorld) {
            serverPlayer.teleport(this.rootedX, this.rootedY, this.rootedZ, false);
            serverPlayer.setVelocity(Vec3d.ZERO);
            serverPlayer.velocityModified = true;
        } else {
            this.player.setVelocity(Vec3d.ZERO);
            this.player.velocityModified = true;
        }

        if (this.rootedTicks % 20 == 0 || this.rootedTicks == 0) {
            this.sync();
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("shieldActive", this.shieldActive);
        tag.putFloat("lockedYaw", this.lockedYaw);
        tag.putFloat("lockedPitch", this.lockedPitch);
        tag.putInt("rootedTicks", this.rootedTicks);
        tag.putDouble("rootedX", this.rootedX);
        tag.putDouble("rootedY", this.rootedY);
        tag.putDouble("rootedZ", this.rootedZ);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.shieldActive = tag.getBoolean("shieldActive");
        this.lockedYaw = tag.getFloat("lockedYaw");
        this.lockedPitch = tag.getFloat("lockedPitch");
        this.rootedTicks = tag.getInt("rootedTicks");
        this.rootedX = tag.getDouble("rootedX");
        this.rootedY = tag.getDouble("rootedY");
        this.rootedZ = tag.getDouble("rootedZ");
    }
}
