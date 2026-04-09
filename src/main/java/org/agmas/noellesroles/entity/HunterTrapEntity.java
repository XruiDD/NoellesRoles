package org.agmas.noellesroles.entity;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.damage.DamageSource;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.hunter.HunterPlayerComponent;
import org.agmas.noellesroles.vulture.VulturePlayerComponent;

import java.util.UUID;

public class HunterTrapEntity extends Entity {
    public static final String EVENT_TRIGGERED = "hunter_trap_triggered";
    private UUID ownerUuid;
    private UUID poisonerUuid;
    private boolean poisoned;
    private int armTicks = 10;

    public HunterTrapEntity(EntityType<? extends HunterTrapEntity> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    public void setOwner(PlayerEntity owner) {
        this.ownerUuid = owner == null ? null : owner.getUuid();
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public boolean isPoisoned() {
        return this.poisoned;
    }

    public void setPoisoned(boolean poisoned) {
        this.poisoned = poisoned;
    }

    public UUID getPoisonerUuid() {
        return this.poisonerUuid;
    }

    public void setPoisonerUuid(UUID poisonerUuid) {
        this.poisonerUuid = poisonerUuid;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.armTicks > 0) {
            this.armTicks--;
        }

        if (this.getWorld().isClient) {
            return;
        }

        this.setVelocity(Vec3d.ZERO);
        this.velocityModified = true;

        BlockPos supportPos = this.getBlockPos().down();
        if (!this.getWorld().getBlockState(supportPos).isSolidBlock(this.getWorld(), supportPos)) {
            this.discard();
            return;
        }

        if (this.age > 20 * 60 * 10) {
            this.discard();
            return;
        }

        if (this.armTicks > 0) {
            return;
        }

        Box triggerBox = this.getBoundingBox().expand(0.35, 0.15, 0.35);
        for (PlayerEntity player : this.getWorld().getEntitiesByClass(PlayerEntity.class, triggerBox, this::canTrigger)) {
            this.trigger(player);
            break;
        }
    }

    private boolean canTrigger(PlayerEntity player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }

        if (this.isVultureSpeeding(player)) {
            return false;
        }
        return true;
    }

    private void trigger(PlayerEntity player) {
        this.recordTrigger(player);
        HunterPlayerComponent component = HunterPlayerComponent.KEY.get(player);
        component.trap();
        component.addFractureLayer();

        if (this.poisoned) {
            PlayerEntity owner = this.ownerUuid == null ? null : this.getWorld().getPlayerByUuid(this.ownerUuid);
            PlayerPoisonComponent poisonComponent = PlayerPoisonComponent.KEY.get(player);
            poisonComponent.setPoisonTicks(20 * 40, owner == null ? null : owner.getUuid(), Noellesroles.POISON_SOURCE_TRAP);
            this.rewardPoisonTrigger();
        }

        this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 0.8F, 0.8F);
        ((net.minecraft.server.world.ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.05, this.getZ(), 8, 0.2, 0.05, 0.2, 0.05);
        this.discard();
    }

    public boolean canBeSeenBy(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.canUseKillerFeatures(player)
            || gameWorld.isRole(player, WatheRoles.VIGILANTE)
            || gameWorld.isRole(player, WatheRoles.VETERAN)
            || gameWorld.isRole(player, Noellesroles.RIOT_PATROL)
            || gameWorld.isRole(player, Noellesroles.CORRUPT_COP)
            || gameWorld.isRole(player, Noellesroles.ENGINEER);
    }

    public boolean canBeRemovedBy(PlayerEntity player) {
        if (player == null || player.getWorld().isClient) {
            return false;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, WatheRoles.VETERAN)
            || gameWorld.isRole(player, Noellesroles.RIOT_PATROL)
            || gameWorld.isRole(player, Noellesroles.CORRUPT_COP)
            || gameWorld.isRole(player, Noellesroles.ENGINEER);
    }

    public ItemStack asPickupStack() {
        return org.agmas.noellesroles.ModItems.HUNTER_TRAP.getDefaultStack();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof PlayerEntity player) || !this.canBeRemovedBy(player)) {
            return false;
        }

        if (!this.getWorld().isClient) {
            this.dropStack(this.asPickupStack());
            this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_CHAIN_FALL, SoundCategory.PLAYERS, 0.8F, 1.2F);
            this.discard();
        }
        return true;
    }

    private void rewardPoisonTrigger() {
        if (this.getWorld().isClient) {
            return;
        }

        if (this.poisonerUuid != null) {
            PlayerEntity poisoner = this.getWorld().getPlayerByUuid(this.poisonerUuid);
            if (poisoner != null) {
                PlayerShopComponent.KEY.get(poisoner).addToBalance(75);
            }
        }

        if (this.ownerUuid != null) {
            PlayerEntity owner = this.getWorld().getPlayerByUuid(this.ownerUuid);
            if (owner != null) {
                PlayerShopComponent.KEY.get(owner).addToBalance(50);
            }
        }
    }

    private void recordTrigger(PlayerEntity player) {
        if (!(this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld)) {
            return;
        }
        if (!(player instanceof ServerPlayerEntity serverTarget)) {
            return;
        }

        var event = GameRecordManager.event(EVENT_TRIGGERED)
            .target(serverTarget);
        PlayerEntity owner = this.ownerUuid == null ? null : serverWorld.getPlayerByUuid(this.ownerUuid);
        if (owner instanceof ServerPlayerEntity serverOwner) {
            event.actor(serverOwner);
        }
        if (this.poisonerUuid != null) {
            event.putUuid("poisoner", this.poisonerUuid);
        }
        event.put("poisoned", Boolean.toString(this.poisoned));
        event.put("x", Double.toString(this.getX()));
        event.put("y", Double.toString(this.getY()));
        event.put("z", Double.toString(this.getZ()));
        event.record();
    }

    private boolean isVultureSpeeding(PlayerEntity player) {
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.VULTURE)) {
            return false;
        }
        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            return true;
        }
        return VulturePlayerComponent.KEY.get(player).getHighlightTicks() > 0;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.ownerUuid = nbt.containsUuid("owner") ? nbt.getUuid("owner") : null;
        this.poisonerUuid = nbt.containsUuid("poisoner") ? nbt.getUuid("poisoner") : null;
        this.poisoned = nbt.getBoolean("poisoned");
        this.armTicks = nbt.getInt("armTicks");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("owner", this.ownerUuid);
        }
        if (this.poisonerUuid != null) {
            nbt.putUuid("poisoner", this.poisonerUuid);
        }
        nbt.putBoolean("poisoned", this.poisoned);
        nbt.putInt("armTicks", this.armTicks);
    }
}
