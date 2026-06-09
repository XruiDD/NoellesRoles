package org.agmas.noellesroles.jester;

import dev.doctor4t.wathe.api.event.PsychoType;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheAttributes;
import dev.doctor4t.wathe.index.WatheEntities;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
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
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.agmas.noellesroles.ModSounds;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.music.MusicMomentType;
import org.agmas.noellesroles.music.WorldMusicComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class JesterPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<JesterPlayerComponent> KEY =
        ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "jester"), JesterPlayerComponent.class);
    private static final Identifier EVENT_MOMENT_START = Identifier.of(Noellesroles.MOD_ID, "jester_moment_start");
    private static final Identifier EVENT_MOMENT_END = Identifier.of(Noellesroles.MOD_ID, "jester_moment_end");
    private static final Identifier PSYCHO_SPRINT_MODIFIER_ID = Identifier.of(Noellesroles.MOD_ID, "jester_psycho_sprint");

    private final PlayerEntity player;
    public boolean won = false;

    public int spectatorTicks = 0;
    private UUID fakeBodyUuid = null;

    public boolean inStasis = false;
    public int stasisTicks = 0;
    private double stasisX = 0, stasisY = 0, stasisZ = 0;

    public boolean inPsychoMode = false;
    public int psychoModeTicks = 0;
    public int psychoArmour = 0;
    public int baseArmour = 1;
    public int killCount = 0;
    public UUID targetKiller = null;

    private double deathX = 0, deathY = 0, deathZ = 0;

    private final Map<UUID, Float> savedMoods = new HashMap<>();

    public JesterPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        if (this.inPsychoMode) {
            return true;
        }
        return player == this.player;
    }

    public boolean isTransitioning() {
        return this.spectatorTicks > 0 || this.inStasis;
    }

    public void reset() {
        // 若在假死旁观阶段被重置，恢复游戏模式与视角，避免卡在旁观
        if (this.spectatorTicks > 0 && player instanceof ServerPlayerEntity specPlayer) {
            specPlayer.setCameraEntity(specPlayer);
            specPlayer.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
        }
        this.won = false;
        this.inStasis = false;
        this.stasisTicks = 0;
        this.spectatorTicks = 0;
        this.killCount = 0;
        this.psychoArmour = 0;
        this.targetKiller = null;

        if (this.fakeBodyUuid != null && player.getWorld() instanceof ServerWorld sw) {
            Entity body = sw.getEntity(this.fakeBodyUuid);
            if (body instanceof PlayerBodyEntity) body.discard();
        }
        this.fakeBodyUuid = null;

        if (!this.savedMoods.isEmpty() && player.getWorld() instanceof ServerWorld sw2) {
            for (Map.Entry<UUID, Float> e : this.savedMoods.entrySet()) {
                ServerPlayerEntity p = sw2.getServer().getPlayerManager().getPlayer(e.getKey());
                if (p != null) PlayerMoodComponent.KEY.get(p).setMood(e.getValue());
            }
        }
        this.savedMoods.clear();

        if (this.inPsychoMode) {
            if (player.getWorld() != null) {
                WorldMusicComponent worldMusic = WorldMusicComponent.KEY.get(player.getWorld());
                worldMusic.stopMusic();
            }
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (this.psychoModeTicks > 0 && player.getWorld() instanceof ServerWorld serverWorld) {
                    NbtCompound extra = new NbtCompound();
                    extra.putString("reason", "killed");
                    extra.putInt("remaining_ticks", this.psychoModeTicks);
                    GameRecordManager.recordGlobalEvent(serverWorld, EVENT_MOMENT_END, serverPlayer, extra);
                }
                EntityAttributeInstance sprintAttr = serverPlayer.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
                if (sprintAttr != null && sprintAttr.hasModifier(PSYCHO_SPRINT_MODIFIER_ID)) {
                    sprintAttr.removeModifier(PSYCHO_SPRINT_MODIFIER_ID);
                }
            }
            this.inPsychoMode = false;
            PlayerPsychoComponent psychoComponent = PlayerPsychoComponent.KEY.get(this.player);
            psychoComponent.stopPsycho();
        }
        this.psychoModeTicks = 0;
        this.sync();
    }

    public void beginFakeDeath(UUID killerUuid) {
        if (!(player instanceof ServerPlayerEntity serverJester)) return;
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        this.targetKiller = killerUuid;
        this.deathX = player.getX();
        this.deathY = player.getY();
        this.deathZ = player.getZ();
        float yaw = serverJester.getHeadYaw();

        PlayerBodyEntity body = WatheEntities.PLAYER_BODY.create(serverWorld);
        if (body != null) {
            body.setPlayerUuid(serverJester.getUuid());
            body.setDeathReason(GameConstants.DeathReasons.GUN);
            body.setDeathGameTime(serverWorld.getTime());
            body.refreshPositionAndAngles(this.deathX, this.deathY, this.deathZ, yaw, 0f);
            body.setYaw(yaw);
            body.setHeadYaw(yaw);
            serverWorld.spawnEntity(body);
            this.fakeBodyUuid = body.getUuid();
        }

        serverJester.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
        if (body != null) serverJester.setCameraEntity(body);
        this.spectatorTicks = GameConstants.getInTicks(0, 5);

        this.sync();
    }

    private void revive() {
        if (!(player instanceof ServerPlayerEntity serverJester)) return;
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        if (this.fakeBodyUuid != null) {
            Entity body = serverWorld.getEntity(this.fakeBodyUuid);
            if (body instanceof PlayerBodyEntity) body.discard();
            this.fakeBodyUuid = null;
        }

        serverJester.setCameraEntity(serverJester);
        serverJester.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
        serverJester.teleport(serverWorld, this.deathX, this.deathY, this.deathZ,
                serverJester.getYaw(), serverJester.getPitch());

        this.enterStasis(GameConstants.getInTicks(0, 3));
    }

    public void enterStasis(int ticks) {
        this.inStasis = true;
        this.stasisTicks = ticks;
        this.stasisX = player.getX();
        this.stasisY = player.getY();
        this.stasisZ = player.getZ();
        this.sync();

        if (player.getWorld() instanceof ServerWorld serverWorld) {
            RegistryEntry<SoundEvent> soundEntry = RegistryEntry.of(ModSounds.JESTER_LAUGH);
            long seed = serverWorld.random.nextLong();
            for (ServerPlayerEntity sp : serverWorld.getServer().getPlayerManager().getPlayerList()) {
                sp.networkHandler.sendPacket(new PlaySoundS2CPacket(
                        soundEntry, SoundCategory.MASTER,
                        sp.getX(), sp.getY(), sp.getZ(), 2.0F, 1.0F, seed));
            }
        }
    }

    private void startJesterPsychoMode() {
        PlayerPsychoComponent psychoComponent = PlayerPsychoComponent.KEY.get(this.player);
        if (psychoComponent.startPsycho(PsychoType.VISIBLE_QUIET)) {
            this.psychoModeTicks = GameConstants.getInTicks(0, 60);
            this.killCount = 0;
            // 初始护盾 = 当局总人数/6（上限 3）
            int totalPlayers = GameWorldComponent.KEY.get(player.getWorld()).getAllPlayers().size();
            this.baseArmour = Math.min(3, totalPlayers / 6);
            this.psychoArmour = this.baseArmour;
            psychoComponent.setPsychoTicks(Integer.MAX_VALUE);
            psychoComponent.setArmour(this.psychoArmour);
            this.inPsychoMode = true;

            if (player instanceof ServerPlayerEntity serverPlayer) {
                EntityAttributeInstance sprintAttr = serverPlayer.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
                if (sprintAttr != null && !sprintAttr.hasModifier(PSYCHO_SPRINT_MODIFIER_ID)) {
                    sprintAttr.addTemporaryModifier(new EntityAttributeModifier(
                        PSYCHO_SPRINT_MODIFIER_ID, 2.0,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                PlayerStaminaComponent staminaComp = PlayerStaminaComponent.KEY.get(player);
                int effectiveMax = sprintAttr != null ? (int) sprintAttr.getValue()
                    : staminaComp.getMaxSprintTime() * 3;
                staminaComp.setSprintingTicks(effectiveMax);
                staminaComp.setMaxSprintTime(effectiveMax);
                staminaComp.setExhausted(false);
                staminaComp.sync();
            }

            if (player.getWorld() instanceof ServerWorld serverWorld) {
                this.savedMoods.clear();
                for (ServerPlayerEntity p : serverWorld.getPlayers()) {
                    PlayerMoodComponent moodComp = PlayerMoodComponent.KEY.get(p);
                    this.savedMoods.put(p.getUuid(), moodComp.getMood());
                    moodComp.setMood(0);
                }
            }

            WorldMusicComponent worldMusic = WorldMusicComponent.KEY.get(this.player.getWorld());
            worldMusic.startMusic(MusicMomentType.JESTER_MOMENT, 1);

            if (player.getWorld() instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer) {
                NbtCompound extra = new NbtCompound();
                extra.putInt("armour", this.psychoArmour);
                if (this.targetKiller != null) extra.putUuid("trigger", this.targetKiller);
                GameRecordManager.recordGlobalEvent(serverWorld, EVENT_MOMENT_START, serverPlayer, extra);

                for (ServerPlayerEntity p : serverWorld.getPlayers()) {
                    p.networkHandler.sendPacket(new TitleS2CPacket(
                        Text.translatable("title.noellesroles.jester_moment")
                            .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)));
                    p.networkHandler.sendPacket(new SubtitleS2CPacket(
                        Text.translatable("subtitle.noellesroles.jester_moment")
                            .formatted(Formatting.LIGHT_PURPLE)));
                    p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 100, 10));
                    p.sendMessage(Text.translatable("title.noellesroles.jester_moment")
                            .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
                }
            }

            this.sync();
        }
    }

    public void registerKill() {
        this.killCount++;
        this.psychoModeTicks += GameConstants.getInTicks(0, 30);
        // 每累计击杀奇数人次(第1、3、5…杀)各 +1 层，上限 3
        this.psychoArmour = Math.min(3, this.baseArmour + (this.killCount + 1) / 2);
        PlayerPsychoComponent psychoComponent = PlayerPsychoComponent.KEY.get(this.player);
        psychoComponent.setArmour(this.psychoArmour);
        this.sync();
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
        if (this.spectatorTicks > 0) {
            this.spectatorTicks--;
            if (this.spectatorTicks == 0) {
                this.revive();
            }
            return;
        }

        if (this.stasisTicks > 0) {
            this.stasisTicks--;
            if (this.player instanceof ServerPlayerEntity serverPlayer
                    && serverPlayer.getWorld() instanceof ServerWorld serverWorld) {
                serverPlayer.teleport(this.stasisX, this.stasisY, this.stasisZ, false);
                serverPlayer.setVelocity(0, 0, 0);
                serverPlayer.velocityModified = true;
                serverWorld.spawnParticles(ParticleTypes.GLOW,
                    serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(),
                    5, 0.5, 0.5, 0.5, 0.02);
            }
            if (this.stasisTicks == 0) {
                this.inStasis = false;
                this.startJesterPsychoMode();
                this.sync();
            }
            return;
        }

        if (this.inPsychoMode && this.psychoModeTicks > 0) {
            this.psychoModeTicks--;
            if (this.psychoModeTicks <= 0) {
                if (player instanceof ServerPlayerEntity serverPlayer && player.getWorld() instanceof ServerWorld serverWorld) {
                    NbtCompound extra = new NbtCompound();
                    extra.putString("reason", "timeout");
                    GameRecordManager.recordGlobalEvent(serverWorld, EVENT_MOMENT_END, serverPlayer, extra);
                }
                GameFunctions.killPlayer((ServerPlayerEntity) this.player, true, null, Noellesroles.DEATH_REASON_JESTER_TIMEOUT, true);
            } else if (this.psychoModeTicks % 20 == 0) {
                // 无限体力：每 20 tick 把小丑冲刺体力回满（配合 startJesterPsychoMode 的较高上限，20 tick 内不会耗尽）
                if (this.player instanceof ServerPlayerEntity) {
                    PlayerStaminaComponent staminaComp = PlayerStaminaComponent.KEY.get(this.player);
                    staminaComp.setSprintingTicks(staminaComp.getMaxSprintTime());
                    staminaComp.setExhausted(false);
                    staminaComp.sync();
                }
                this.sync();
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("won", this.won);
        tag.putInt("spectatorTicks", this.spectatorTicks);
        tag.putBoolean("inStasis", this.inStasis);
        tag.putInt("stasisTicks", this.stasisTicks);
        tag.putBoolean("inPsychoMode", this.inPsychoMode);
        tag.putInt("psychoModeTicks", this.psychoModeTicks);
        tag.putInt("psychoArmour", this.psychoArmour);
        tag.putInt("baseArmour", this.baseArmour);
        tag.putInt("killCount", this.killCount);
        tag.putDouble("stasisX", this.stasisX);
        tag.putDouble("stasisY", this.stasisY);
        tag.putDouble("stasisZ", this.stasisZ);
        tag.putDouble("deathX", this.deathX);
        tag.putDouble("deathY", this.deathY);
        tag.putDouble("deathZ", this.deathZ);
        if (this.targetKiller != null) tag.putUuid("targetKiller", this.targetKiller);
        if (this.fakeBodyUuid != null) tag.putUuid("fakeBodyUuid", this.fakeBodyUuid);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.won = tag.getBoolean("won");
        this.spectatorTicks = tag.getInt("spectatorTicks");
        this.inStasis = tag.getBoolean("inStasis");
        this.stasisTicks = tag.getInt("stasisTicks");
        this.inPsychoMode = tag.getBoolean("inPsychoMode");
        this.psychoModeTicks = tag.getInt("psychoModeTicks");
        this.psychoArmour = tag.getInt("psychoArmour");
        this.baseArmour = tag.contains("baseArmour") ? tag.getInt("baseArmour") : 1;
        this.killCount = tag.getInt("killCount");
        this.stasisX = tag.getDouble("stasisX");
        this.stasisY = tag.getDouble("stasisY");
        this.stasisZ = tag.getDouble("stasisZ");
        this.deathX = tag.getDouble("deathX");
        this.deathY = tag.getDouble("deathY");
        this.deathZ = tag.getDouble("deathZ");
        this.targetKiller = tag.containsUuid("targetKiller") ? tag.getUuid("targetKiller") : null;
        this.fakeBodyUuid = tag.containsUuid("fakeBodyUuid") ? tag.getUuid("fakeBodyUuid") : null;
    }
}
