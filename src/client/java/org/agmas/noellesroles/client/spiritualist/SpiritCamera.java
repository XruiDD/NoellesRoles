package org.agmas.noellesroles.client.spiritualist;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.cca.MapEnhancementsWorldComponent;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.MovementConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.Packet;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.spiritualist.SpiritPlayerComponent;

import java.util.Collections;
import java.util.UUID;

/**
 * 通灵者灵魂出窍用的假玩家实体
 * 继承 ClientPlayerEntity，通过 MC.setCameraEntity(this) 接管相机
 * 使用 MC 原生飞行物理（abilities.flying + travel），无需手动计算坐标
 */
public class SpiritCamera extends ClientPlayerEntity {

    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final double MAX_RADIUS = 30.0;

    private double bodyX, bodyY, bodyZ;

    private static ClientPlayNetworkHandler createDummyHandler() {
        return new ClientPlayNetworkHandler(
                MC,
                MC.getNetworkHandler().getConnection(),
                new ClientConnectionState(
                        new GameProfile(UUID.randomUUID(), "SpiritCamera"),
                        MC.getTelemetryManager().createWorldSession(false, null, null),
                        DynamicRegistryManager.Immutable.EMPTY,
                        FeatureSet.empty(),
                        null,
                        MC.getCurrentServerEntry(),
                        MC.currentScreen,
                        Collections.emptyMap(),
                        MC.inGameHud.getChatHud().toChatState(),
                        false,
                        Collections.emptyMap(),
                        ServerLinks.EMPTY
                )
        ) {
            @Override
            public void sendPacket(Packet<?> packet) {
            }
        };
    }

    public SpiritCamera(int id) {
        super(MC, MC.world, createDummyHandler(), MC.player.getStatHandler(), MC.player.getRecipeBook(), false, false);
        setId(id);
        noClip = true;
        setPose(EntityPose.SWIMMING);
        getAbilities().flying = true;
        getAbilities().allowFlying = true;
        input = new KeyboardInput(MC.options);
    }

    public void applyPosition(Entity entity) {
        refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
        renderPitch = getPitch();
        renderYaw = getYaw();
        lastRenderPitch = renderPitch;
        lastRenderYaw = renderYaw;
    }

    public void spawn() {
        if (clientWorld != null) {
            clientWorld.addEntity(this);
        }
    }

    public void despawn() {
        if (clientWorld != null && clientWorld.getEntityById(getId()) != null) {
            clientWorld.removeEntity(getId(), RemovalReason.DISCARDED);
        }
    }

    public void setBodyPosition(double x, double y, double z) {
        this.bodyX = x;
        this.bodyY = y;
        this.bodyZ = z;
    }

    // --- 禁用不需要的物理效果 ---

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
    }

    @Override
    public boolean isClimbing() {
        return false;
    }

    @Override
    public boolean isTouchingWater() {
        return false;
    }

    @Override
    public boolean collidesWith(Entity other) {
        return false;
    }

    // 不可被其他实体碰撞检测到
    @Override
    public boolean isCollidable() {
        return false;
    }

    // 不可被推动
    @Override
    public boolean isPushable() {
        return false;
    }

    // 让碰撞系统彻底忽略此实体（EntityPredicates.EXCEPT_SPECTATOR 会过滤观察者）
    @Override
    public boolean isSpectator() {
        return true;
    }

    // 灵体不能疾跑
    @Override
    public void setSprinting(boolean sprinting) {
        super.setSprinting(false);
    }

    @Override
    public void setPose(EntityPose pose) {
        super.setPose(EntityPose.SWIMMING);
    }

    @Override
    public boolean shouldSlowDown() {
        return false;
    }

    // --- 移动逻辑：使用 MC 原生飞行物理 ---

    @Override
    public void tickMovement() {
        noClip = true;
        getAbilities().flying = true;

        // 从 wathe 配置读取行走速度，用作飞行速度
        float flySpeed = getWatheWalkSpeed();
        getAbilities().setFlySpeed(flySpeed);

        // 调用 super：处理 input.tick()、鼠标视角、原生飞行移动（travel）
        super.tickMovement();

        setOnGround(false);

        // 边界限制：super 已经根据 input 移动了位置，我们在之后做裁剪
        clampPosition();
    }

    /**
     * 从 wathe 地图配置读取行走速度，转换为等效飞行速度
     *
     * 地面稳态速度 = movementSpeed / (1 - groundFriction)  = movementSpeed / 0.4
     * 飞行稳态速度 = flySpeed     / (1 - airDrag)          = flySpeed / 0.09
     *
     * 令两者相等: flySpeed = movementSpeed * 0.09 / 0.4 = movementSpeed * 0.225
     * wathe 行走 movementSpeed = 0.07 * walkMultiplier
     */
    private static final float FLY_SPEED_FACTOR = 0.09f / 0.4f; // 0.225

    private float getWatheWalkSpeed() {
        try {
            MovementConfig movement = MapEnhancementsWorldComponent.KEY.get(MC.world).getMovementConfig();
            return 0.07f * movement.walkSpeedMultiplier() * FLY_SPEED_FACTOR;
        } catch (Exception e) {
            return 0.07f * FLY_SPEED_FACTOR;
        }
    }

    /**
     * 限制灵魂位置在 play area 和本体 30 格半径内
     */
    private void clampPosition() {
        double newX = getX();
        double newY = getY();
        double newZ = getZ();
        boolean clamped = false;

        // Play area 边界
        try {
            MapVariablesWorldComponent mapVars = MapVariablesWorldComponent.KEY.get(MC.world);
            Box playArea = mapVars.getPlayArea();
            if (playArea != null) {
                double cx = MathHelper.clamp(newX, playArea.minX, playArea.maxX);
                double cy = MathHelper.clamp(newY, playArea.minY, playArea.maxY);
                double cz = MathHelper.clamp(newZ, playArea.minZ, playArea.maxZ);
                if (cx != newX || cy != newY || cz != newZ) {
                    newX = cx;
                    newY = cy;
                    newZ = cz;
                    clamped = true;
                }
            }
        } catch (Exception ignored) {
        }

        // 半径限制
        double dx = newX - bodyX;
        double dy = newY - bodyY;
        double dz = newZ - bodyZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > MAX_RADIUS) {
            double scale = MAX_RADIUS / dist;
            newX = bodyX + dx * scale;
            newY = bodyY + dy * scale;
            newZ = bodyZ + dz * scale;
            clamped = true;
        }

        if (clamped) {
            setPosition(newX, newY, newZ);
        }
    }
}
