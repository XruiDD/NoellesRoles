package org.agmas.noellesroles.detective;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.*;

/**
 * 世界级组件：记录游戏中的击杀事件，供侦探查验使用。
 * 通过 KillPlayer.AFTER 事件注册，记录每次有 killer 的击杀。
 * 时间戳使用 world.getTime()（游戏 tick）。
 */
public class KillHistoryWorldComponent implements ServerTickingComponent {
    public static final ComponentKey<KillHistoryWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "kill_history"),
            KillHistoryWorldComponent.class
    );

    // 2分钟 = 2400 ticks
    private static final int MAX_LOOKBACK_TICKS = GameConstants.getInTicks(2, 0);

    // 免疫的 deathReason：毒杀、炸弹、刺客猜人
    private static final Set<Identifier> IMMUNE_DEATH_REASONS = Set.of(
            GameConstants.DeathReasons.POISON,
            Noellesroles.DEATH_REASON_BOMB,
            Noellesroles.DEATH_REASON_ASSASSINATED
    );

    private final World world;
    private final List<KillRecord> killRecords = new ArrayList<>();

    public KillHistoryWorldComponent(World world) {
        this.world = world;
    }

    /**
     * 记录一次击杀事件
     */
    public void recordKill(UUID killerUuid, UUID victimUuid, Identifier deathReason) {
        killRecords.add(new KillRecord(killerUuid, victimUuid, deathReason, world.getTime()));
    }

    /**
     * 查询指定玩家在 lookbackTicks 内是否有非免疫击杀记录
     */
    public boolean hasRecentNonImmuneKill(UUID playerUuid, int lookbackTicks, long currentTick) {
        for (KillRecord record : killRecords) {
            if (record.killerUuid.equals(playerUuid)
                    && (currentTick - record.timestampTicks) <= lookbackTicks
                    && !IMMUNE_DEATH_REASONS.contains(record.deathReason)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清空所有记录（游戏重新开始时调用）
     */
    public void reset() {
        killRecords.clear();
    }

    @Override
    public void serverTick() {
        if (!killRecords.isEmpty()) {
            long currentTick = world.getTime();
            killRecords.removeIf(record -> (currentTick - record.timestampTicks) > MAX_LOOKBACK_TICKS);
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (KillRecord record : killRecords) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("killer", record.killerUuid);
            entry.putUuid("victim", record.victimUuid);
            entry.putString("deathReason", record.deathReason.toString());
            entry.putLong("tick", record.timestampTicks);
            list.add(entry);
        }
        tag.put("killRecords", list);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        killRecords.clear();
        if (tag.contains("killRecords")) {
            NbtList list = tag.getList("killRecords", NbtCompound.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound entry = list.getCompound(i);
                killRecords.add(new KillRecord(
                        entry.getUuid("killer"),
                        entry.getUuid("victim"),
                        Identifier.of(entry.getString("deathReason")),
                        entry.getLong("tick")
                ));
            }
        }
    }

    private record KillRecord(UUID killerUuid, UUID victimUuid, Identifier deathReason, long timestampTicks) {
    }
}
