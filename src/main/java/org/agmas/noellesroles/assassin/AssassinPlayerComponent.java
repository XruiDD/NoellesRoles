package org.agmas.noellesroles.assassin;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class AssassinPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<AssassinPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "assassin"), AssassinPlayerComponent.class);
    private final PlayerEntity player;

    // 核心字段（仅存储游戏逻辑数据，不存储UI状态）
    private int guessesRemaining = 0;      // 剩余猜测次数
    private int maxGuesses = 0;            // 最大猜测次数 = (totalPlayers + 3) / 4
    private int cooldownTicks = 0;         // 冷却时间 (1200 ticks = 60秒)

    // 常量
    public static final int COOLDOWN_TICKS = GameConstants.getInTicks(1, 0); // 60秒

    public AssassinPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.guessesRemaining = 0;
        this.maxGuesses = 0;
        this.cooldownTicks = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    // Getters
    public int getGuessesRemaining() {
        return guessesRemaining;
    }

    public int getMaxGuesses() {
        return maxGuesses;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    // Setters
    public void setMaxGuesses(int playerCount) {
        this.maxGuesses = (playerCount + 3) / 4;  // 向上取整
        this.guessesRemaining = this.maxGuesses;
        this.sync();
    }

    public void setCooldown(int ticks) {
        this.cooldownTicks = ticks;
        this.sync();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    /**
     * 检查是否可以使用猜测技能
     * @return 如果有剩余次数且不在冷却中，返回true
     */
    public boolean canGuess() {
        return guessesRemaining > 0 && cooldownTicks <= 0;
    }

    /**
     * 使用一次猜测，扣除次数并设置冷却
     */
    public void useGuess() {
        if (guessesRemaining > 0) {
            guessesRemaining--;
        }
        cooldownTicks = COOLDOWN_TICKS;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            if (cooldownTicks % 20 == 0) {
                this.sync();
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("guessesRemaining", this.guessesRemaining);
        tag.putInt("maxGuesses", this.maxGuesses);
        tag.putInt("cooldownTicks", this.cooldownTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.guessesRemaining = tag.contains("guessesRemaining") ? tag.getInt("guessesRemaining") : 0;
        this.maxGuesses = tag.contains("maxGuesses") ? tag.getInt("maxGuesses") : 0;
        this.cooldownTicks = tag.contains("cooldownTicks") ? tag.getInt("cooldownTicks") : 0;
    }
}
