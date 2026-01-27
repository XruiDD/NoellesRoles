package org.agmas.noellesroles.bomber;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheParticles;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModSounds;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;


import java.util.UUID;

import static org.agmas.noellesroles.ModItems.TIMED_BOMB;


/**
 * 炸弹客玩家组件
 * 管理炸弹状态：放置、滴滴声、传递冷却、爆炸
 */
public class BomberPlayerComponent implements ServerTickingComponent {
    public static final ComponentKey<BomberPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "bomber"), BomberPlayerComponent.class);

    private final PlayerEntity player;

    // 炸弹是否被放置在此玩家身上
    private boolean hasBomb = false;
    // 炸弹计时器（tick）- 从放置到滴滴声开始
    private int bombTimer = 0;
    // 滴滴声计时器（tick）- 从滴滴声开始到爆炸
    private int beepTimer = 0;
    // 传递冷却（tick）
    private int transferCooldown = 0;
    // 是否正在滴滴声阶段
    private boolean isBeeping = false;
    // 放置炸弹的炸弹客UUID
    private UUID bomberUuid = null;
    // 上一次显示的倒计时秒数（用于避免每tick发送消息）
    private int lastDisplayedSeconds = -1;

    // 常量
    public static final int BOMB_DELAY_TICKS = GameConstants.getInTicks(0, 10); // 10秒后开始滴滴声
    public static final int BEEP_DURATION_TICKS = GameConstants.getInTicks(0, 15); // 滴滴声持续15秒后爆炸
    public static final int TRANSFER_COOLDOWN_TICKS = GameConstants.getInTicks(0, 3); // 3秒传递冷却
    public static final int BEEP_INTERVAL_TICKS = 6;

    public BomberPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        if(hasBomb){
            removeBombFromInventory(player);
        }
        this.hasBomb = false;
        this.bombTimer = 0;
        this.beepTimer = 0;
        this.transferCooldown = 0;
        this.isBeeping = false;
        this.bomberUuid = null;
        this.lastDisplayedSeconds = -1;
    }

    /**
     * 在此玩家身上放置炸弹
     * @param bomber 放置炸弹的炸弹客
     */
    public void placeBomb(PlayerEntity bomber) {
        this.hasBomb = true;
        this.bombTimer = BOMB_DELAY_TICKS;
        this.beepTimer = 0;
        this.isBeeping = false;
        this.bomberUuid = bomber.getUuid();
        this.transferCooldown = 0;
        this.lastDisplayedSeconds = -1;
    }

    /**
     * 尝试将炸弹传递给另一个玩家
     *
     * @param target 目标玩家
     */
    public void transferBomb(PlayerEntity target) {
        if (!hasBomb || !isBeeping || transferCooldown > 0) {
            return;
        }

        // 检查目标是否有效
        if (target == null || target == this.player) {
            return;
        }

        // 检查目标是否存活
        if (!GameFunctions.isPlayerAliveAndSurvival(target) || SwallowedPlayerComponent.isPlayerSwallowed(target)) {
            return;
        }

        // 获取目标的组件
        BomberPlayerComponent targetComponent = KEY.get(target);

        // 如果目标已经有炸弹，不能传递
        if (targetComponent.hasBomb) {
            return;
        }

        // 传递炸弹状态
        targetComponent.hasBomb = true;
        targetComponent.bombTimer = 0;
        targetComponent.beepTimer = this.beepTimer;
        targetComponent.isBeeping = true;
        targetComponent.bomberUuid = this.bomberUuid;
        targetComponent.transferCooldown = TRANSFER_COOLDOWN_TICKS;
        targetComponent.lastDisplayedSeconds = -1;

        // 清除自己的炸弹
        this.hasBomb = false;
        this.bombTimer = 0;
        this.beepTimer = 0;
        this.isBeeping = false;
        this.lastDisplayedSeconds = -1;

        // 移除自己物品栏中的炸弹物品
        removeBombFromInventory(this.player);

        // 给目标物品栏添加炸弹物品
        target.giveItemStack(TIMED_BOMB.getDefaultStack());

        // 播放传递音效
        player.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * 从玩家物品栏移除炸弹物品
     */
    private void removeBombFromInventory(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(TIMED_BOMB)) {
                player.getInventory().removeStack(i, 1);
                break;
            }
        }
    }

    @Override
    public void serverTick() {
        if (!hasBomb) return;

        // 传递冷却递减
        if (transferCooldown > 0) {
            transferCooldown--;
        }

        if (!isBeeping) {
            // 放置后等待阶段
            if (bombTimer > 0) {
                bombTimer--;
            } else {
                // 开始滴滴声阶段
                isBeeping = true;
                transferCooldown = TRANSFER_COOLDOWN_TICKS;
                beepTimer = BEEP_DURATION_TICKS;
                player.giveItemStack(TIMED_BOMB.getDefaultStack());
            }
        } else {
            // 滴滴声阶段
            if (beepTimer > 0) {
                // 每0.5秒播放滴滴声
                if (beepTimer % BEEP_INTERVAL_TICKS == 0) {
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.BOMB_BEEP, SoundCategory.PLAYERS, 2.0F, 1.0F);
                }

                int secondsLeft = (beepTimer + 19) / 20;
                if (secondsLeft != lastDisplayedSeconds) {
                    lastDisplayedSeconds = secondsLeft;
                    player.sendMessage(Text.translatable("tip.bomber.bomb_warning", secondsLeft)
                            .formatted(Formatting.RED, Formatting.BOLD), true);
                }

                beepTimer--;
            } else {
                // 爆炸！
                explode();
            }
        }
    }

    /**
     * 炸弹爆炸
     */
    private void explode() {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        // 移除炸弹物品
        removeBombFromInventory(player);

        // 播放爆炸音效
        serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.BOMB_EXPLODE, SoundCategory.PLAYERS, 3.0F, 1.0F);

        // 生成粒子特效
        // 大爆炸粒子
        serverWorld.spawnParticles(WatheParticles.BIG_EXPLOSION,
                player.getX(), player.getY() + 0.5, player.getZ(),
                1, 0, 0, 0, 0);

        // 烟雾粒子
        serverWorld.spawnParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 0.5, player.getZ(),
                100, 0, 0, 0, 0.2);

        // 炸弹物品碎片粒子
        serverWorld.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, TIMED_BOMB.getDefaultStack()),
                player.getX(), player.getY() + 0.5, player.getZ(),
                100, 0, 0, 0, 1.0);

        // 获取炸弹客玩家
        PlayerEntity bomber = bomberUuid != null ? player.getWorld().getPlayerByUuid(bomberUuid) : null;

        // 检查持有者是否被饕餮吞噬
        SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(player);
        if (swallowedComp.isSwallowed()) {
            // 炸弹在肚子里爆炸，杀死饕餮而非持有者
            UUID taotieUuid = swallowedComp.getSwallowedBy();
            if (taotieUuid != null) {
                PlayerEntity taotie = serverWorld.getPlayerByUuid(taotieUuid);
                if (taotie != null && GameFunctions.isPlayerAliveAndSurvival(taotie)) {
                    GameFunctions.killPlayer(taotie, true, bomber, Noellesroles.DEATH_REASON_BOMB);
                }
            }
        } else {
            // 普通情况：杀死携带炸弹的玩家
            if (GameFunctions.isPlayerAliveAndSurvival(player)) {
                GameFunctions.killPlayer(player, true, bomber, Noellesroles.DEATH_REASON_BOMB);
            }
        }

        // 重置状态
        this.hasBomb = false;
        this.bombTimer = 0;
        this.beepTimer = 0;
        this.isBeeping = false;
        this.lastDisplayedSeconds = -1;
    }

    public UUID getBomberUuid() {
        return bomberUuid;
    }

    // Getters
    public boolean hasBomb() {
        return hasBomb;
    }

    public boolean isBeeping() {
        return isBeeping;
    }

    public int getBeepTimer() {
        return beepTimer;
    }

    public int getTransferCooldown() {
        return transferCooldown;
    }

    public boolean canTransfer(PlayerEntity target) {
        return hasBomb && isBeeping && transferCooldown <= 0 && !(KEY.get(target).hasBomb);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("hasBomb", this.hasBomb);
        tag.putInt("bombTimer", this.bombTimer);
        tag.putInt("beepTimer", this.beepTimer);
        tag.putInt("transferCooldown", this.transferCooldown);
        tag.putBoolean("isBeeping", this.isBeeping);
        if (this.bomberUuid != null) {
            tag.putUuid("bomberUuid", this.bomberUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.hasBomb = tag.getBoolean("hasBomb");
        this.bombTimer = tag.getInt("bombTimer");
        this.beepTimer = tag.getInt("beepTimer");
        this.transferCooldown = tag.getInt("transferCooldown");
        this.isBeeping = tag.getBoolean("isBeeping");
        if (tag.containsUuid("bomberUuid")) {
            this.bomberUuid = tag.getUuid("bomberUuid");
        } else {
            this.bomberUuid = null;
        }
    }
}
