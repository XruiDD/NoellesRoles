package org.agmas.noellesroles.scavenger;

import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 清道夫角色组件
 * 特性：
 * - 杀人后尸体对其他人不可见（除了秃鹫）
 * - 杀人奖励额外+50金币（总共150）
 * - 只能购买刀，不能买其他攻击性武器
 * - 特殊能力：花100金币重置刀的冷却
 */
public class ScavengerPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<ScavengerPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "scavenger"), ScavengerPlayerComponent.class);

    private final PlayerEntity player;

    // 存储清道夫杀死的玩家UUID列表，用于隐藏对应的尸体
    private final List<UUID> hiddenBodies;

    public ScavengerPlayerComponent(PlayerEntity player) {
        this.player = player;
        this.hiddenBodies = new ArrayList<>();
    }

    /**
     * 重置组件状态（游戏开始时调用）
     */
    public void reset() {
        this.hiddenBodies.clear();
        this.sync();
    }

    /**
     * 添加一个需要隐藏的尸体
     * @param victimUuid 被杀玩家的UUID
     */
    public void addHiddenBody(UUID victimUuid) {
        if (!this.hiddenBodies.contains(victimUuid)) {
            this.hiddenBodies.add(victimUuid);
            this.sync();
        }
    }

    /**
     * 检查某个尸体是否应该对指定玩家隐藏
     * @param victimUuid 尸体对应的玩家UUID
     * @return 是否隐藏
     */
    public boolean isBodyHidden(UUID victimUuid) {
        return this.hiddenBodies.contains(victimUuid);
    }

    /**
     * 获取所有隐藏的尸体UUID列表
     */
    public List<UUID> getHiddenBodies() {
        return new ArrayList<>(this.hiddenBodies);
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        // 保存隐藏尸体列表
        NbtCompound bodiesTag = new NbtCompound();
        for (int i = 0; i < hiddenBodies.size(); i++) {
            bodiesTag.putUuid("body_" + i, hiddenBodies.get(i));
        }
        bodiesTag.putInt("count", hiddenBodies.size());
        tag.put("hiddenBodies", bodiesTag);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        // 读取隐藏尸体列表
        this.hiddenBodies.clear();
        if (tag.contains("hiddenBodies")) {
            NbtCompound bodiesTag = tag.getCompound("hiddenBodies");
            int count = bodiesTag.getInt("count");
            for (int i = 0; i < count; i++) {
                if (bodiesTag.contains("body_" + i)) {
                    this.hiddenBodies.add(bodiesTag.getUuid("body_" + i));
                }
            }
        }
    }
}
