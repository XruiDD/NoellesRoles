package org.agmas.noellesroles.entity;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.Noellesroles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.*;

/**
 * 毒气云实体
 * - BFS扩散系统，最多500个方块
 * - 在毒气中停留5秒(100 ticks)将中毒
 * - 30秒(600 ticks)后消散
 */
public class PoisonGasCloudEntity extends Entity {
    private static final int MAX_GAS_BLOCKS = 500;
    private static final int MAX_LIFETIME = 600; // 30秒
    private static final int SPREAD_INTERVAL = 8; // 每8 ticks扩散一次，约10秒填满5x5x20车厢
    private static final int EXPOSURE_THRESHOLD = 100; // 5秒暴露阈值
    private static final double MAX_SPREAD_RADIUS_SQ = 20.0 * 20.0; // 最大扩散半径20格（平方）
    private static final DustParticleEffect GAS_PARTICLE = new DustParticleEffect(new Vector3f(0.3f, 0.8f, 0.2f), 1.5f);

    private final Set<BlockPos> gasBlocks = new HashSet<>();
    private Set<BlockPos> frontier = new HashSet<>();
    private final Map<UUID, Integer> exposureTicks = new HashMap<>();
    private UUID ownerUuid;
    private int age = 0;

    public PoisonGasCloudEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (age > MAX_LIFETIME) {
            this.discard();
            return;
        }

        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        // 初始化起始位置
        if (age == 1) {
            BlockPos startPos = this.getBlockPos();
            gasBlocks.add(startPos);
            frontier.add(startPos);
        }

        // BFS扩散（被阻挡的边缘方块保留在frontier中，支持门打开后继续扩散）
        if (age % SPREAD_INTERVAL == 0 && !frontier.isEmpty() && gasBlocks.size() < MAX_GAS_BLOCKS) {
            Set<BlockPos> newFrontier = new HashSet<>();
            for (BlockPos pos : frontier) {
                if (gasBlocks.size() >= MAX_GAS_BLOCKS) break;
                boolean stillEdge = false;
                for (Direction direction : Direction.values()) {
                    if (gasBlocks.size() >= MAX_GAS_BLOCKS) break;
                    BlockPos neighbor = pos.offset(direction);
                    if (gasBlocks.contains(neighbor)) continue;
                    // 超出最大扩散半径则跳过
                    if (neighbor.getSquaredDistance(this.getBlockPos()) > MAX_SPREAD_RADIUS_SQ) continue;
                    VoxelShape fromShape = serverWorld.getBlockState(pos).getCollisionShape(serverWorld, pos);
                    VoxelShape toShape = serverWorld.getBlockState(neighbor).getCollisionShape(serverWorld, neighbor);
                    // 源方块出口检测（方向性）+ 目标方块入口检测（体积+面）
                    if (doesShapeBlockExit(fromShape, direction)
                        || isBlockTooSolid(toShape)
                        || doesShapeBlockEntry(toShape, direction)) {
                        stillEdge = true; // 有被阻挡的邻居，保留在frontier中等待重新检测
                        continue;
                    }
                    gasBlocks.add(neighbor);
                    newFrontier.add(neighbor);
                }
                if (stillEdge) {
                    newFrontier.add(pos); // 保留边缘方块，门打开后可继续扩散
                }
            }
            frontier = newFrontier;
        }

        // 玩家中毒检测（毒师免疫）
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverWorld);
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (!GameFunctions.isPlayerAliveAndSurvival(player)) continue;
            if (gameWorld.isRole(player, Noellesroles.POISONER)) continue;

            Box box = player.getBoundingBox();
            boolean inGas = false;
            for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX) && !inGas; x++) {
                for (int y = MathHelper.floor(box.minY); y <= MathHelper.floor(box.maxY) && !inGas; y++) {
                    for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ) && !inGas; z++) {
                        if (gasBlocks.contains(new BlockPos(x, y, z))) {
                            inGas = true;
                        }
                    }
                }
            }
            if (inGas) {
                int ticks = exposureTicks.getOrDefault(player.getUuid(), 0) + 1;
                exposureTicks.put(player.getUuid(), ticks);

                if (ticks >= EXPOSURE_THRESHOLD) {
                    PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(player);
                    if (poisonComp.poisonTicks <= 0) {
                        int poisonTime = PlayerPoisonComponent.clampTime.getLeft() +
                                serverWorld.random.nextInt(PlayerPoisonComponent.clampTime.getRight() - PlayerPoisonComponent.clampTime.getLeft() + 1);
                        NbtCompound recordExtra = new NbtCompound();
                        recordExtra.putString("source", "gas_bomb");
                        poisonComp.setPoisonTicks(poisonTime, ownerUuid, recordExtra);
                        exposureTicks.put(player.getUuid(), 0);
                    }
                }
            } else {
                exposureTicks.put(player.getUuid(), 0);
            }
        }

        // 粒子效果
        if (!gasBlocks.isEmpty()) {
            List<BlockPos> blockList = new ArrayList<>(gasBlocks);
            int particleCount = 4 + serverWorld.random.nextInt(3); // 4-6个粒子
            for (int i = 0; i < particleCount && !blockList.isEmpty(); i++) {
                BlockPos pos = blockList.get(serverWorld.random.nextInt(blockList.size()));
                serverWorld.spawnParticles(
                        GAS_PARTICLE,
                        pos.getX() + 0.5 + serverWorld.random.nextGaussian() * 0.3,
                        pos.getY() + 0.5 + serverWorld.random.nextGaussian() * 0.3,
                        pos.getZ() + 0.5 + serverWorld.random.nextGaussian() * 0.3,
                        1, 0, 0, 0, 0
                );
            }
        }
    }

    private double getCrossSection(VoxelShape shape, Direction.Axis moveAxis) {
        Direction.Axis perp1, perp2;
        switch (moveAxis) {
            case X -> { perp1 = Direction.Axis.Y; perp2 = Direction.Axis.Z; }
            case Y -> { perp1 = Direction.Axis.X; perp2 = Direction.Axis.Z; }
            default -> { perp1 = Direction.Axis.X; perp2 = Direction.Axis.Y; }
        }
        return (shape.getMax(perp1) - shape.getMin(perp1)) * (shape.getMax(perp2) - shape.getMin(perp2));
    }

    /**
     * 出口检测：气体能否从源方块的某个面离开
     * (a) 碰撞箱覆盖出口面 → 阻挡（屏障面板等贴面方块）
     * (b) 碰撞箱形成中间墙壁（深度>0.1）→ 阻挡（门等中部方块）
     * 均要求垂直截面覆盖率 > 50%
     */
    private boolean doesShapeBlockExit(VoxelShape shape, Direction direction) {
        if (shape.isEmpty()) return false;

        Direction.Axis moveAxis = direction.getAxis();
        if (getCrossSection(shape, moveAxis) <= 0.5) return false;

        // (a) 碰撞箱覆盖出口面
        boolean reachesFace = direction.getDirection() == Direction.AxisDirection.POSITIVE
                ? shape.getMax(moveAxis) > 0.99
                : shape.getMin(moveAxis) < 0.01;
        if (reachesFace) return true;

        // (b) 中间墙壁（深度>0.1，如门 4/16=0.25）
        double depth = shape.getMax(moveAxis) - shape.getMin(moveAxis);
        return depth > 0.1;
    }

    /**
     * 入口体积检测：完整实心方块不允许气体进入
     * 包围盒体积 >= 0.5 视为实心（石头、玻璃、半砖等）
     */
    private boolean isBlockTooSolid(VoxelShape shape) {
        if (shape.isEmpty()) return false;
        double x = shape.getMax(Direction.Axis.X) - shape.getMin(Direction.Axis.X);
        double y = shape.getMax(Direction.Axis.Y) - shape.getMin(Direction.Axis.Y);
        double z = shape.getMax(Direction.Axis.Z) - shape.getMin(Direction.Axis.Z);
        return x * y * z >= 0.5;
    }

    /**
     * 入口面检测：碰撞箱是否覆盖了气体进入的那个面
     * 处理屏障面板等只挡一面的薄方块
     *
     * 气体沿moveDirection移动进入目标方块，入口面在方块的反方向侧：
     * - 气体向SOUTH(+Z)移动 → 入口面在z=0(min端)
     * - 气体向NORTH(-Z)移动 → 入口面在z=1(max端)
     */
    private boolean doesShapeBlockEntry(VoxelShape shape, Direction moveDirection) {
        if (shape.isEmpty()) return false;

        Direction.Axis moveAxis = moveDirection.getAxis();
        if (getCrossSection(shape, moveAxis) <= 0.5) return false;

        if (moveDirection.getDirection() == Direction.AxisDirection.POSITIVE) {
            return shape.getMin(moveAxis) < 0.01;
        } else {
            return shape.getMax(moveAxis) > 0.99;
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (ownerUuid != null) {
            nbt.putUuid("OwnerUuid", ownerUuid);
        }
        nbt.putInt("Age", age);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("OwnerUuid")) {
            ownerUuid = nbt.getUuid("OwnerUuid");
        }
        age = nbt.getInt("Age");
    }
}
