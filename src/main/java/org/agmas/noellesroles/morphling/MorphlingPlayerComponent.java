package org.agmas.noellesroles.morphling;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

public class MorphlingPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<MorphlingPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "morphling"), MorphlingPlayerComponent.class);
    private final PlayerEntity player;
    public UUID disguise;
    public int morphTicks = 0;
    public boolean corpseMode = false;

    public void reset() {
        this.corpseMode = false;
        this.stopMorph();
        this.sync();
    }

    public MorphlingPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient){
        if (recipient == this.player) {
            buf.writeInt(this.morphTicks);
            buf.writeUuid(this.disguise == null ? player.getUuid() : this.disguise);
        } else {
            buf.writeInt(this.morphTicks > 0 ? 1 : 0);
            buf.writeUuid(this.disguise == null ? player.getUuid() : this.disguise);
        }
        buf.writeBoolean(this.corpseMode);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.morphTicks = buf.readInt();
        this.disguise = buf.readUuid();
        this.corpseMode = buf.readBoolean();
    }

    public void serverTick() {
        if (this.morphTicks > 0 && disguise != null) {
            // 只在目标完全离线时停止变形，不再检查 GameMode（允许变形为已死亡的旁观者）
            if (player.getWorld().getPlayerByUuid(disguise) == null) {
                stopMorph();
                return;
            }
            if (--this.morphTicks == 0) {
                this.stopMorph();
            }
            if (this.morphTicks %20 == 0) {
                this.sync();
            }
        }

        if (this.morphTicks < 0) {
            if (++this.morphTicks % 20 == 0) {
                this.sync();
            }
        }

        // 尸体模式下给予缓慢III效果，使移动速度接近下蹲
        if (this.corpseMode) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 2, 2, true, false, false));
        }
    }

    public boolean startMorph(UUID id) {
        setMorphTicks(GameConstants.getInTicks(0,35));
        disguise = id;
        this.sync();
        return true;
    }

    public void stopMorph() {
        this.morphTicks = -GameConstants.getInTicks(0,20);
        this.sync();
    }

    public void toggleCorpseMode() {
        this.corpseMode = !this.corpseMode;
        this.sync();
    }

    public int getMorphTicks() {
        return this.morphTicks;
    }

    public void setMorphTicks(int ticks) {
        this.morphTicks = ticks;
        this.sync();
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("morphTicks", this.morphTicks);
        if (disguise == null) disguise = player.getUuid();
        tag.putUuid("disguise", this.disguise);
        tag.putBoolean("corpseMode", this.corpseMode);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.morphTicks = tag.contains("morphTicks") ? tag.getInt("morphTicks") : 0;
        this.disguise = tag.contains("disguise") ? tag.getUuid("disguise") : player.getUuid();
        this.corpseMode = tag.contains("corpseMode") && tag.getBoolean("corpseMode");
    }
}
