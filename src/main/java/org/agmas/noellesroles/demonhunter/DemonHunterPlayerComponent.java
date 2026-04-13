package org.agmas.noellesroles.demonhunter;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 猎魔人角色组件 — 跟踪疯魔玩家列表，供客户端高亮渲染。
 */
public class DemonHunterPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<DemonHunterPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "demon_hunter"), DemonHunterPlayerComponent.class);

    private final PlayerEntity player;

    /** 当前处于公开疯魔状态的玩家 UUID 列表（仅 trackActive=true 的） */
    private final List<UUID> frenzyPlayerUuids = new ArrayList<>();

    public DemonHunterPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    // ── 疯魔玩家追踪 ──

    public void addFrenzyPlayer(UUID uuid) {
        if (!frenzyPlayerUuids.contains(uuid)) {
            frenzyPlayerUuids.add(uuid);
            sync();
        }
    }

    public void removeFrenzyPlayer(UUID uuid) {
        if (frenzyPlayerUuids.remove(uuid)) {
            sync();
        }
    }

    public boolean isPlayerFrenzied(UUID uuid) {
        return frenzyPlayerUuids.contains(uuid);
    }

    public List<UUID> getFrenzyPlayerUuids() {
        return frenzyPlayerUuids;
    }

    // ── 生命周期 ──

    public void reset() {
        frenzyPlayerUuids.clear();
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void serverTick() {
        // 当没有公开疯魔进行中时，移除猎魔枪
        if (!(this.player instanceof ServerPlayerEntity)) return;
        GameWorldComponent game = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!game.isRole(this.player, Noellesroles.DEMON_HUNTER)) return;

        if (!frenzyPlayerUuids.isEmpty() && !game.isPsychoActive()) {
            // psychosActive 已归零但列表还有残留 → 清理
            frenzyPlayerUuids.clear();
            DemonHunterPistolItem.removePistol(this.player);
            sync();
        }
    }

    // ── 网络同步 ──

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(frenzyPlayerUuids.size());
        for (UUID uuid : frenzyPlayerUuids) {
            buf.writeUuid(uuid);
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        frenzyPlayerUuids.clear();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            frenzyPlayerUuids.add(buf.readUuid());
        }
    }

    // ── NBT 持久化 ──

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (UUID uuid : frenzyPlayerUuids) {
            list.add(NbtString.of(uuid.toString()));
        }
        tag.put("frenzyPlayers", list);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        frenzyPlayerUuids.clear();
        if (tag.contains("frenzyPlayers")) {
            NbtList list = tag.getList("frenzyPlayers", NbtString.of("").getType());
            for (int i = 0; i < list.size(); i++) {
                try {
                    frenzyPlayerUuids.add(UUID.fromString(list.getString(i)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
