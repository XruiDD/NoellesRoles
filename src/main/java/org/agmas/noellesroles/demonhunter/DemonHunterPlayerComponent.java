package org.agmas.noellesroles.demonhunter;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
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

    /** 当前处于可见疯魔状态的玩家 UUID 列表（PsychoType.PUBLIC 或 VISIBLE_QUIET） */
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
        // 当追踪列表中所有疯魔玩家都已退出疯魔状态时，移除猎魔枪
        if (!(this.player instanceof ServerPlayerEntity)) return;
        GameWorldComponent game = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!game.isRole(this.player, Noellesroles.DEMON_HUNTER)) return;
        if (frenzyPlayerUuids.isEmpty()) return;

        // 逐个检查追踪的玩家是否仍在疯魔（psychoTicks > 0）
        // 不再依赖 GameWorldComponent.isPsychoActive()，因为 VISIBLE_QUIET 类型（如小丑疯魔）不计入该计数器
        boolean anyStillFrenzied = false;
        for (UUID uuid : frenzyPlayerUuids) {
            PlayerEntity tracked = this.player.getWorld().getPlayerByUuid(uuid);
            if (tracked != null && PlayerPsychoComponent.KEY.get(tracked).psychoTicks > 0) {
                anyStillFrenzied = true;
                break;
            }
        }
        if (!anyStillFrenzied) {
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
