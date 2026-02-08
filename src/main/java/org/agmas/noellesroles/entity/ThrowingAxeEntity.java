package org.agmas.noellesroles.entity;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.NoellesRolesEntities;

import org.jetbrains.annotations.Nullable;
import java.util.HashSet;
import java.util.Set;

public class ThrowingAxeEntity extends PersistentProjectileEntity {

    private static final TrackedData<Byte> DATA_HIT_DIRECTION = DataTracker.registerData(ThrowingAxeEntity.class, TrackedDataHandlerRegistry.BYTE);

    private static final int MAX_LIFETIME = 20 * 120; // 120 seconds

    @Nullable
    private BlockPos stuckBlockPos = null;
    @Nullable
    private Direction stuckDirection = null;
    private int ticksAlive = 0;
    private boolean isStuck = false;
    private final Set<Integer> hitEntities = new HashSet<>();

    public ThrowingAxeEntity(EntityType<? extends ThrowingAxeEntity> entityType, World world) {
        super(entityType, world);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_HIT_DIRECTION, (byte) 0);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItems.THROWING_AXE);
    }

    @Override
    public void tick() {
        this.ticksAlive++;

        // Auto-cleanup after max lifetime
        if (this.ticksAlive > MAX_LIFETIME) {
            this.discard();
            return;
        }

        // If stuck in block, don't call super.tick() physics
        if (this.isStuck) {
            // 方块被破坏时丢弃飞斧
            if (!this.getWorld().isClient && this.stuckBlockPos != null
                    && this.getWorld().getBlockState(this.stuckBlockPos).isAir()) {
                this.discard();
            }
            return;
        }

        // 自定义贯穿：命中飞行路径上的所有玩家，而不是只命中最近的一个
        if (!this.getWorld().isClient) {
            Vec3d currentPos = this.getPos();
            Vec3d velocity = this.getVelocity();
            Vec3d nextPos = currentPos.add(velocity);

            Box searchBox = this.getBoundingBox().stretch(velocity).expand(1.0);
            for (Entity entity : this.getWorld().getOtherEntities(this, searchBox)) {
                if (!(entity instanceof ServerPlayerEntity)) continue;
                if (!entity.canBeHitByProjectile()) continue;
                if (hitEntities.contains(entity.getId())) continue;

                Box entityBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
                if (entityBox.raycast(currentPos, nextPos).isPresent()) {
                    hitEntities.add(entity.getId());
                    this.onEntityHit(new EntityHitResult(entity));
                }
            }
        }

        super.tick();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        this.stuckBlockPos = blockHitResult.getBlockPos();
        this.stuckDirection = blockHitResult.getSide();
        this.dataTracker.set(DATA_HIT_DIRECTION, (byte) this.stuckDirection.getId());

        Vec3d hitLocation = blockHitResult.getPos();
        this.setPosition(hitLocation.x, hitLocation.y, hitLocation.z);
        this.setVelocity(Vec3d.ZERO);
        this.isStuck = true;

        this.playSound(SoundEvents.ITEM_TRIDENT_HIT_GROUND, 1.0F, 1.0F);
    }

    /**
     * 始终返回null，实体碰撞由tick()中的自定义贯穿逻辑处理。
     * 父类只处理方块碰撞。
     */
    @Nullable
    @Override
    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        return null;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();

        // Only kill players
        if (!(entity instanceof ServerPlayerEntity target)) return;

        // Must be alive and in game
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) return;

        Entity owner = this.getOwner();
        // Don't hit the thrower
        if (owner != null && target.getUuid().equals(owner.getUuid())) return;

        // Instant kill using game's kill function
        ServerPlayerEntity killerPlayer = (owner instanceof ServerPlayerEntity sp) ? sp : null;
        GameFunctions.killPlayer(target, true, killerPlayer, Noellesroles.DEATH_REASON_THROWING_AXE);

        // Play hit sound but do NOT stop — continue flying for penetration
        this.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.0F, 1.0F);

        // Slight speed reduction on penetration (keep most momentum)
        this.setVelocity(this.getVelocity().multiply(0.9, 0.9, 0.9));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Age", this.ticksAlive);
        nbt.putBoolean("IsStuck", this.isStuck);
        if (this.stuckDirection != null) {
            nbt.putByte("HitDirection", (byte) this.stuckDirection.getId());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.ticksAlive = nbt.getInt("Age");
        this.isStuck = nbt.getBoolean("IsStuck");
        if (nbt.contains("HitDirection")) {
            this.stuckDirection = Direction.byId(nbt.getByte("HitDirection"));
            this.dataTracker.set(DATA_HIT_DIRECTION, nbt.getByte("HitDirection"));
        }
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public int getTicksAlive() {
        return this.ticksAlive;
    }

    public boolean isStuckInBlock() {
        return this.isStuck || this.stuckBlockPos != null;
    }

    public Direction getHitDirection() {
        if (this.stuckDirection != null) {
            return this.stuckDirection;
        }
        byte dirValue = this.dataTracker.get(DATA_HIT_DIRECTION);
        return Direction.byId(dirValue);
    }

    @Nullable
    public BlockPos getStuckBlockPos() {
        return this.stuckBlockPos;
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        return false; // Never pickable
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        // No pickup interaction
    }
}
