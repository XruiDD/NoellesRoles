package org.agmas.noellesroles.scavenger;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 世界级组件：存储被清道夫隐藏的尸体UUID集合
 * 解决玩家组件在清道夫离线后状态丢失的问题
 */
public class HiddenBodiesWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<HiddenBodiesWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "hidden_bodies"),
            HiddenBodiesWorldComponent.class
    );

    private final World world;
    private final Set<UUID> hiddenBodies = new HashSet<>();

    public HiddenBodiesWorldComponent(World world) {
        this.world = world;
    }

    public void addHiddenBody(UUID victimUuid) {
        if (this.hiddenBodies.add(victimUuid)) {
            KEY.sync(this.world);
        }
    }

    public boolean isHidden(UUID victimUuid) {
        return this.hiddenBodies.contains(victimUuid);
    }

    public void reset() {
        if (!this.hiddenBodies.isEmpty()) {
            this.hiddenBodies.clear();
            KEY.sync(this.world);
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(hiddenBodies.size());
        for (UUID uuid : hiddenBodies) {
            buf.writeUuid(uuid);
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.hiddenBodies.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            this.hiddenBodies.add(buf.readUuid());
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound bodiesTag = new NbtCompound();
        int i = 0;
        for (UUID uuid : hiddenBodies) {
            bodiesTag.putUuid("body_" + i, uuid);
            i++;
        }
        bodiesTag.putInt("count", hiddenBodies.size());
        tag.put("hiddenBodies", bodiesTag);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
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
