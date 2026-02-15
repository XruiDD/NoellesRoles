package org.agmas.noellesroles.recaller;

import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class RecallerPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<RecallerPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "recaller"), RecallerPlayerComponent.class);
    private final PlayerEntity player;
    public boolean placed = false;
    public double x = 0;
    public double y = 0;
    public double z = 0;

    public void reset() {
        this.placed = false;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.sync();
    }

    public RecallerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    public void setPosition() {
        x = player.getX();
        y = player.getY();
        z = player.getZ();
        placed = true;
        this.sync();
    }


    public void teleport() {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // 传送前在原位播放粒子和音效
            serverPlayer.getWorld().sendEntityStatus(serverPlayer, EntityStatuses.ADD_PORTAL_PARTICLES);
            serverPlayer.getWorld().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            // 无条件传送（不走 LivingEntity.teleport 的安全检查）
            serverPlayer.requestTeleport(x, y, z);
            // 传送后在目标位置再播放一次音效
            serverPlayer.getWorld().playSound(null, x, y, z,
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        placed = false;
        this.sync();
    }


    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putDouble("x", this.x);
        tag.putDouble("y", this.y);
        tag.putDouble("z", this.z);
        tag.putBoolean("placed", this.placed);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.x = tag.contains("x") ? tag.getDouble("x") : 0;
        this.y = tag.contains("y") ? tag.getDouble("y") : 0;
        this.z = tag.contains("z") ? tag.getDouble("z") : 0;
        this.placed = tag.contains("placed") && tag.getBoolean("placed");
    }
}
