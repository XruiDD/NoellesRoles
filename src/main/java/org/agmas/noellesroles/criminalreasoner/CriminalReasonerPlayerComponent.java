package org.agmas.noellesroles.criminalreasoner;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class CriminalReasonerPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<CriminalReasonerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "criminal_reasoner"), CriminalReasonerPlayerComponent.class);

    private final PlayerEntity player;
    private final Map<UUID, UUID> victimToKiller = new HashMap<>();
    private final Set<UUID> solvedVictims = new HashSet<>();
    private int successfulReasoningCount;

    public CriminalReasonerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.victimToKiller.clear();
        this.solvedVictims.clear();
        this.successfulReasoningCount = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void recordReasoningTarget(UUID victimUuid, UUID killerUuid) {
        if (victimUuid == null || killerUuid == null) {
            return;
        }

        // 保存每名死者最后一次有效击杀者，供犯罪推理学家后续进行匹配。
        this.victimToKiller.put(victimUuid, killerUuid);
        this.sync();
    }

    public boolean isCorrectReasoning(UUID victimUuid, UUID suspectUuid) {
        if (victimUuid == null || suspectUuid == null) {
            return false;
        }

        UUID actualKiller = this.victimToKiller.get(victimUuid);
        return suspectUuid.equals(actualKiller);
    }

    public boolean recordSuccessfulReasoning(UUID victimUuid) {
        if (victimUuid == null || this.solvedVictims.contains(victimUuid)) {
            return false;
        }

        // 同一名死者只记一次成功推理，避免重复推理刷胜利进度。
        this.solvedVictims.add(victimUuid);
        this.successfulReasoningCount++;
        this.sync();
        return true;
    }

    public int getSuccessfulReasoningCount() {
        return this.successfulReasoningCount;
    }

    public boolean hasSolvedVictim(UUID victimUuid) {
        return victimUuid != null && this.solvedVictims.contains(victimUuid);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        // 只同步 successfulReasoningCount 和 solvedVictims，不同步 victimToKiller 以防作弊客户端读取答案。
        buf.writeInt(this.successfulReasoningCount);
        buf.writeInt(this.solvedVictims.size());
        for (UUID solved : this.solvedVictims) {
            buf.writeUuid(solved);
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.successfulReasoningCount = buf.readInt();
        this.solvedVictims.clear();
        int solvedCount = buf.readInt();
        for (int i = 0; i < solvedCount; i++) {
            this.solvedVictims.add(buf.readUuid());
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        this.victimToKiller.clear();
        this.solvedVictims.clear();
        this.successfulReasoningCount = nbtCompound.getInt("successfulReasoningCount");
        if (nbtCompound.contains("victimToKiller")) {
            NbtCompound mappingTag = nbtCompound.getCompound("victimToKiller");
            for (String key : mappingTag.getKeys()) {
                try {
                    UUID victimUuid = UUID.fromString(key);
                    UUID killerUuid = mappingTag.getUuid(key);
                    this.victimToKiller.put(victimUuid, killerUuid);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (nbtCompound.contains("solvedVictims")) {
            NbtList solvedList = nbtCompound.getList("solvedVictims", NbtString.STRING_TYPE);
            for (int i = 0; i < solvedList.size(); i++) {
                try {
                    this.solvedVictims.add(UUID.fromString(solvedList.getString(i)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        NbtCompound mappingTag = new NbtCompound();
        for (Map.Entry<UUID, UUID> entry : this.victimToKiller.entrySet()) {
            mappingTag.putUuid(entry.getKey().toString(), entry.getValue());
        }
        nbtCompound.put("victimToKiller", mappingTag);

        NbtList solvedList = new NbtList();
        for (UUID solvedVictim : this.solvedVictims) {
            solvedList.add(NbtString.of(solvedVictim.toString()));
        }
        nbtCompound.put("solvedVictims", solvedList);
        nbtCompound.putInt("successfulReasoningCount", this.successfulReasoningCount);
    }
}
