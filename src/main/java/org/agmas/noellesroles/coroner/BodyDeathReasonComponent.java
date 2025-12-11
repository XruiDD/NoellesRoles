package org.agmas.noellesroles.coroner;

import dev.doctor4t.trainmurdermystery.entity.PlayerBodyEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 尸体组件 - 仅用于秃鹫功能
 * <p>
 * 死亡原因和角色信息已由主模组 PlayerBodyEntity 处理
 */
public class BodyDeathReasonComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<BodyDeathReasonComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "body_death_reason"), BodyDeathReasonComponent.class);
    public boolean vultured = false;
    public PlayerBodyEntity playerBodyEntity;

    public BodyDeathReasonComponent(PlayerBodyEntity playerBodyEntity) {
        this.playerBodyEntity = playerBodyEntity;
    }

    public void sync() {
        KEY.sync(this.playerBodyEntity);
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("vultured", vultured);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.vultured = tag.getBoolean("vultured");
    }

    @Override
    public void serverTick() {
        this.sync();
    }
}
