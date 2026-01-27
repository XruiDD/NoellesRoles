package org.agmas.noellesroles.jester;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.UUID;
import org.agmas.noellesroles.ModSounds;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class JesterPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<JesterPlayerComponent> KEY =
        ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "jester"), JesterPlayerComponent.class);

    private final PlayerEntity player;
    public boolean won = false;
    public boolean inStasis = false;  // 禁锢状态
    public int stasisTicks = 0;  // 禁锢倒计时
    public int psychoArmour = 0;  // 疯魔模式盾数量
    public boolean inPsychoMode = false;  // 是否在疯魔模式中
    public int psychoModeTicks = 0;  // 疯魔模式剩余时间（3分钟 = 3600 ticks）
    public UUID targetKiller = null;  // 目标击杀者（小丑需要复仇的对象）

    // 禁锢位置记录
    private double stasisX = 0;
    private double stasisY = 0;
    private double stasisZ = 0;

    public JesterPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    public void reset() {
        this.won = false;
        this.inStasis = false;
        this.stasisTicks = 0;
        this.psychoArmour = 0;
        this.psychoModeTicks = 0;
        this.targetKiller = null;
        // 如果正在疯魔模式中，停止疯魔模式
        if (this.inPsychoMode) {
            this.inPsychoMode = false;
            PlayerPsychoComponent psychoComponent = PlayerPsychoComponent.KEY.get(this.player);
            psychoComponent.stopPsycho();
        }
        this.sync();
    }


    public void enterStasis(int ticks) {
        this.inStasis = true;
        this.stasisTicks = ticks;

        // 记录禁锢位置
        this.stasisX = player.getX();
        this.stasisY = player.getY();
        this.stasisZ = player.getZ();

        this.sync();

        // 播放全服声音
        if (player.getWorld() instanceof ServerWorld serverWorld) {

            RegistryEntry<net.minecraft.sound.SoundEvent> soundEntry = RegistryEntry.of(ModSounds.JESTER_LAUGH);

            var seed = serverWorld.random.nextLong();

            for (ServerPlayerEntity serverPlayer : serverWorld.getServer().getPlayerManager().getPlayerList()) {
                serverPlayer.networkHandler.sendPacket(new PlaySoundS2CPacket(
                        soundEntry,
                        SoundCategory.MASTER,
                        serverPlayer.getX(),
                        serverPlayer.getY(),
                        serverPlayer.getZ(),
                        2.0F,
                        1.0F,
                        seed
                ));
            }
        }
    }

    private void startJesterPsychoMode() {
        if (this.psychoArmour <= 0) return;

        PlayerPsychoComponent psychoComponent = PlayerPsychoComponent.KEY.get(this.player);
        if (psychoComponent.startPsycho()) {
            // 设置疯魔模式持续3分钟（3 * 60 * 20 = 3600 ticks）
            this.psychoModeTicks = 3600;
            psychoComponent.setPsychoTicks(Integer.MAX_VALUE);
            psychoComponent.setArmour(this.psychoArmour);
            this.inPsychoMode = true;

            // Broadcast Jester Psycho Mode to all players with Title
            if (player.getWorld() instanceof ServerWorld serverWorld) {
                for (ServerPlayerEntity p : serverWorld.getPlayers()) {
                    // Send Title (主标题)
                    p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
                        net.minecraft.text.Text.translatable("title.noellesroles.jester_moment")
                            .formatted(net.minecraft.util.Formatting.LIGHT_PURPLE, net.minecraft.util.Formatting.BOLD)
                    ));

                    // Send Subtitle (副标题)
                    p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
                        net.minecraft.text.Text.translatable("subtitle.noellesroles.jester_moment")
                            .formatted(Formatting.LIGHT_PURPLE)
                    ));

                    // Set Title times (fadeIn, stay, fadeOut in ticks)
                    p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(
                        10, 100, 10
                    ));

                    // Also send chat message
                    p.sendMessage(net.minecraft.text.Text.translatable("title.noellesroles.jester_moment")
                            .formatted(net.minecraft.util.Formatting.LIGHT_PURPLE, net.minecraft.util.Formatting.BOLD), false);
                }
            }

            this.sync();
        }
    }

    public void sync() {
        KEY.sync(this.player);
    }
    @Override
    public void clientTick() {
        if (this.inPsychoMode && this.psychoModeTicks > 0) {
            this.psychoModeTicks--;

        }
    }

    @Override
    public void serverTick() {
        if (this.stasisTicks > 0) {
            this.stasisTicks--;

            if (this.player instanceof ServerPlayerEntity serverPlayer) {
                if (serverPlayer.getWorld() instanceof ServerWorld serverWorld) {
                    // 位置锁定
                    serverPlayer.teleport(this.stasisX, this.stasisY, this.stasisZ, false);
                    serverPlayer.setVelocity(0, 0, 0);
                    serverPlayer.velocityModified = true;

                    // 粒子特效
                    serverWorld.spawnParticles(ParticleTypes.GLOW,
                        serverPlayer.getX(),
                        serverPlayer.getY() + 1.0,
                        serverPlayer.getZ(),
                        5, 0.5, 0.5, 0.5, 0.02);
                }
            }

            if (this.stasisTicks == 0) {
                this.inStasis = false;
                this.startJesterPsychoMode();
                this.sync();
            }
        }

        // 疯魔模式计时
        if (this.inPsychoMode && this.psychoModeTicks > 0) {
            this.psychoModeTicks--;
            if (this.psychoModeTicks <= 0) {
                GameFunctions.killPlayer(this.player, true, null, Noellesroles.DEATH_REASON_JESTER_TIMEOUT, true);
                this.reset();
            } else if (this.psychoModeTicks % 20 == 0) {
                this.sync();
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("won", this.won);
        tag.putBoolean("inStasis", this.inStasis);
        tag.putInt("stasisTicks", this.stasisTicks);
        tag.putInt("psychoArmour", this.psychoArmour);
        tag.putBoolean("inPsychoMode", this.inPsychoMode);
        tag.putInt("psychoModeTicks", this.psychoModeTicks);
        tag.putDouble("stasisX", this.stasisX);
        tag.putDouble("stasisY", this.stasisY);
        tag.putDouble("stasisZ", this.stasisZ);
        if (this.targetKiller != null) {
            tag.putUuid("targetKiller", this.targetKiller);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.won = tag.getBoolean("won");
        this.inStasis = tag.getBoolean("inStasis");
        this.stasisTicks = tag.getInt("stasisTicks");
        this.psychoArmour = tag.getInt("psychoArmour");
        this.inPsychoMode = tag.getBoolean("inPsychoMode");
        this.psychoModeTicks = tag.getInt("psychoModeTicks");
        this.stasisX = tag.getDouble("stasisX");
        this.stasisY = tag.getDouble("stasisY");
        this.stasisZ = tag.getDouble("stasisZ");
        if (tag.containsUuid("targetKiller")) {
            this.targetKiller = tag.getUuid("targetKiller");
        }else {
            this.targetKiller = null;
        }
    }

}
