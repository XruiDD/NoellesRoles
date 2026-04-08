package org.agmas.noellesroles.riotpatrol;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
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

import java.util.UUID;

public class RiotPatrolPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<RiotPatrolPlayerComponent> KEY = ComponentRegistry.getOrCreate(
        Identifier.of(Noellesroles.MOD_ID, "riot_patrol"), RiotPatrolPlayerComponent.class
    );

    private final PlayerEntity player;
    private boolean shieldActive = false;
    private int rootedTicks = 0;
    private UUID rootLinkedPlayer;
    private boolean rootMaintainer = false;

    public RiotPatrolPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.shieldActive = false;
        this.rootedTicks = 0;
        this.rootLinkedPlayer = null;
        this.rootMaintainer = false;
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
        buf.writeInt(this.rootedTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.shieldActive = buf.readBoolean();
        this.rootedTicks = buf.readInt();
    }

    public void raiseShield() {
        if (this.shieldActive) {
            return;
        }
        this.shieldActive = true;
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

    public void rootAtCurrentPosition(int ticks) {
        this.rootedTicks = Math.max(this.rootedTicks, ticks);
        if (this.rootedTicks <= 0) {
            this.rootLinkedPlayer = null;
            this.rootMaintainer = false;
        }
        this.sync();
    }

    public void startForkRoot(PlayerEntity linkedPlayer, int ticks, boolean maintainer) {
        this.rootedTicks = Math.max(this.rootedTicks, ticks);
        this.rootLinkedPlayer = linkedPlayer == null ? null : linkedPlayer.getUuid();
        this.rootMaintainer = maintainer;
        this.sync();
    }

    public void clearRoot() {
        if (this.rootedTicks == 0 && this.rootLinkedPlayer == null && !this.rootMaintainer) {
            return;
        }
        this.rootedTicks = 0;
        this.rootLinkedPlayer = null;
        this.rootMaintainer = false;
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
            }
        }

        if (this.rootedTicks <= 0) {
            return;
        }

        if (this.rootMaintainer && !this.player.getMainHandStack().isOf(ModItems.RIOT_FORK)) {
            this.clearRootPair();
            return;
        }

        this.rootedTicks--;
        this.player.setVelocity(Vec3d.ZERO);
        this.player.velocityModified = true;

        if (this.rootedTicks == 0) {
            this.clearRootPair();
            return;
        }

        if (this.rootedTicks % 20 == 0) {
            this.sync();
        }
    }

    private void clearRootPair() {
        UUID linkedUuid = this.rootLinkedPlayer;
        this.clearRoot();

        if (linkedUuid == null || this.player.getWorld() == null) {
            return;
        }

        PlayerEntity linkedPlayer = this.player.getWorld().getPlayerByUuid(linkedUuid);
        if (linkedPlayer == null || linkedPlayer == this.player) {
            return;
        }

        RiotPatrolPlayerComponent linkedComponent = KEY.get(linkedPlayer);
        if (linkedComponent.rootedTicks > 0 || linkedComponent.rootLinkedPlayer != null || linkedComponent.rootMaintainer) {
            linkedComponent.clearRoot();
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("shieldActive", this.shieldActive);
        tag.putInt("rootedTicks", this.rootedTicks);
        if (this.rootLinkedPlayer != null) {
            tag.putUuid("rootLinkedPlayer", this.rootLinkedPlayer);
        }
        tag.putBoolean("rootMaintainer", this.rootMaintainer);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.shieldActive = tag.getBoolean("shieldActive");
        this.rootedTicks = tag.getInt("rootedTicks");
        this.rootLinkedPlayer = tag.containsUuid("rootLinkedPlayer") ? tag.getUuid("rootLinkedPlayer") : null;
        this.rootMaintainer = tag.getBoolean("rootMaintainer");
    }
}
