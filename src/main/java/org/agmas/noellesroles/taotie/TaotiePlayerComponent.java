package org.agmas.noellesroles.taotie;

import org.agmas.noellesroles.voice.NoellesrolesVoiceChatPlugin;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.pathogen.InfectedPlayerComponent;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.sound.SoundCategory;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main component for Taotie player
 * Manages swallowed players, cooldowns, and Taotie Moment
 */
public class TaotiePlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<TaotiePlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "taotie"), TaotiePlayerComponent.class);

    // Constants
    public static final int SWALLOW_COOLDOWN = GameConstants.getInTicks(0, 45); // 45 seconds
    public static final int SWALLOW_DISTANCE_SQUARED = 9; // 3 blocks squared
    public static final int TAOTIE_MOMENT_DURATION = GameConstants.getInTicks(2, 0); // 2 minutes

    private final PlayerEntity player;

    // Swallowed players list
    private final List<UUID> swallowedPlayers = new ArrayList<>();

    // Client-side swallowed count (used for display only, not actual UUIDs)
    private int swallowedCountForClient = 0;

    // Cooldown for swallowing (ticks)
    private int swallowCooldown = 0;

    // Taotie Moment state
    private boolean taotieMomentActive = false;
    private int taotieMomentTicks = 0;
    private int triggerThreshold = 0; // Number of players needed to trigger moment

    // Total player count at game start (for win condition)
    private int totalPlayersAtStart = 0;

    // 计算后的吞噬冷却时间（根据玩家数量动态调整）
    private int calculatedSwallowCooldown = SWALLOW_COOLDOWN;

    public TaotiePlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        // Release all swallowed players first
        if (player.getWorld() instanceof ServerWorld) {
            releaseAllPlayers(player.getPos());
        }

        this.swallowedPlayers.clear();
        this.swallowedCountForClient = 0;
        this.swallowCooldown = 0;
        this.taotieMomentActive = false;
        this.taotieMomentTicks = 0;
        this.triggerThreshold = 0;
        this.totalPlayersAtStart = 0;
        this.calculatedSwallowCooldown = SWALLOW_COOLDOWN;

        // Clean up voice chat group
        NoellesrolesVoiceChatPlugin.clearStomachGroup(player.getUuid());

        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == this.player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeInt(this.swallowCooldown);
        buf.writeInt(this.swallowedPlayers.size());
        buf.writeBoolean(this.taotieMomentActive);
        buf.writeInt(this.taotieMomentTicks);
        buf.writeInt(this.totalPlayersAtStart);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.swallowCooldown = buf.readInt();
        this.swallowedCountForClient = buf.readInt();
        this.taotieMomentActive = buf.readBoolean();
        this.taotieMomentTicks = buf.readInt();
        this.totalPlayersAtStart = buf.readInt();
    }

    /**
     * Initialize Taotie for a new game
     */
    public void initializeForGame(int totalPlayers) {
        this.totalPlayersAtStart = totalPlayers;
        // Trigger threshold = totalPlayers / 5 (minimum 2)
        this.triggerThreshold = Math.max(2, totalPlayers / 5);

        // 计算动态冷却（线性）: 在10-40人之间线性插值，10人=50秒，40人=20秒
        // 公式: cooldown = 50 - (playerCount - 10) * (50 - 20) / (40 - 10) = 50 - (playerCount - 10)
        // 简化: cooldown = 60 - playerCount, 限制在20-50秒之间
        int cooldownSeconds = Math.max(20, Math.min(50, 60 - totalPlayers));
        this.calculatedSwallowCooldown = GameConstants.getInTicks(0, cooldownSeconds);

        this.swallowCooldown = 0;
        this.taotieMomentActive = false;
        this.taotieMomentTicks = 0;
        this.swallowedPlayers.clear();
        this.sync();
    }

    /**
     * Attempt to swallow a target player
     * @return true if successful
     */
    public boolean swallowPlayer(ServerPlayerEntity target) {
        if (swallowCooldown > 0) return false;
        if (!(player instanceof ServerPlayerEntity taotie)) return false;

        // Check distance
        if (taotie.squaredDistanceTo(target) > SWALLOW_DISTANCE_SQUARED) return false;

        // Check line of sight
        if (!taotie.canSee(target)) return false;

        // Check if target is already swallowed
        SwallowedPlayerComponent targetSwallowed = SwallowedPlayerComponent.KEY.get(target);
        if (targetSwallowed.isSwallowed()) return false;

        // Check if target is alive (must be in survival or adventure mode)
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) return false;

        // 铁人药水保护
        IronManPlayerComponent ironManComp = IronManPlayerComponent.KEY.get(target);
        if (ironManComp.hasBuff()) {
            ironManComp.removeBuff();
            target.getWorld().playSound(null, target.getBlockPos(),
                WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.MASTER, 5.0F, 1.0F);
            swallowCooldown = calculatedSwallowCooldown; // 正常冷却时间
            this.sync();
            return false;
        }

        // Perform swallow
        targetSwallowed.setSwallowed(taotie.getUuid());
        swallowedPlayers.add(target.getUuid());

        // Set up voice chat group
        NoellesrolesVoiceChatPlugin.addToStomachGroup(taotie, target);

        // Set cooldown (使用动态计算的冷却时间)
        swallowCooldown = calculatedSwallowCooldown;

        // 通知连环杀手目标被吞噬
        if (target.getWorld() instanceof ServerWorld serverWorld) {
            notifySerialKillersTargetSwallowed(target, serverWorld);
        }

        // 病原体感染传播：检查新被吞者或饕餮是否被感染，如果是则传播给肚子里所有人
        spreadInfectionOnSwallow(target);

        this.sync();
        return true;
    }

    /**
     * Notify serial killers if their target was swallowed
     */
    private void notifySerialKillersTargetSwallowed(ServerPlayerEntity target, ServerWorld serverWorld) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(serverWorld);
        for (UUID uuid : gameWorldComponent.getAllWithRole(Noellesroles.SERIAL_KILLER)) {
            PlayerEntity serialKiller = serverWorld.getPlayerByUuid(uuid);
            if (serialKiller != null && GameFunctions.isPlayerAliveAndSurvival(serialKiller)) {
                SerialKillerPlayerComponent serialKillerComp = SerialKillerPlayerComponent.KEY.get(serialKiller);
                if (serialKillerComp.isCurrentTarget(target.getUuid())) {
                    serialKillerComp.onTargetDeath(gameWorldComponent);
                }
            }
        }
    }

    /**
     * Spread infection when a new player is swallowed
     * If anyone (Taotie or any swallowed player) is infected, spread to all
     */
    private void spreadInfectionOnSwallow(ServerPlayerEntity newlySwallowed) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        UUID pathogenUuid = null;

        // Check if Taotie is infected
        InfectedPlayerComponent taotieInfected = InfectedPlayerComponent.KEY.get(player);
        if (taotieInfected.isInfected()) {
            pathogenUuid = taotieInfected.getInfectedBy();
        }

        // Check if newly swallowed player is infected
        if (pathogenUuid == null) {
            InfectedPlayerComponent newlySwallowedInfected = InfectedPlayerComponent.KEY.get(newlySwallowed);
            if (newlySwallowedInfected.isInfected()) {
                pathogenUuid = newlySwallowedInfected.getInfectedBy();
            }
        }

        // Check if any previously swallowed player is infected
        if (pathogenUuid == null) {
            for (UUID uuid : swallowedPlayers) {
                if (uuid.equals(newlySwallowed.getUuid())) continue; // Skip newly swallowed (already checked)
                PlayerEntity swallowed = serverWorld.getPlayerByUuid(uuid);
                if (swallowed != null) {
                    InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(swallowed);
                    if (infected.isInfected()) {
                        pathogenUuid = infected.getInfectedBy();
                        break;
                    }
                }
            }
        }

        // If anyone is infected, spread to all
        if (pathogenUuid != null) {
            spreadInfectionInStomach(pathogenUuid);
        }
    }

    /**
     * Spread infection to all players in Taotie's stomach (including Taotie)
     * Called when infection is detected or when a new infected player is swallowed
     */
    public void spreadInfectionInStomach(UUID pathogenUuid) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;
        if (pathogenUuid == null) return;

        // Infect Taotie
        InfectedPlayerComponent taotieInfected = InfectedPlayerComponent.KEY.get(player);
        if (!taotieInfected.isInfected()) {
            taotieInfected.setInfected(true, pathogenUuid);
        }

        // Infect all swallowed players
        for (UUID uuid : swallowedPlayers) {
            PlayerEntity swallowed = serverWorld.getPlayerByUuid(uuid);
            if (swallowed != null) {
                InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(swallowed);
                if (!infected.isInfected()) {
                    infected.setInfected(true, pathogenUuid);
                }
            }
        }
    }


    /**
     * Remove a specific swallowed player from the list (used when they die while swallowed)
     */
    public void removeSwallowedPlayer(ServerPlayerEntity player) {
        swallowedPlayers.remove(player.getUuid());
        NoellesrolesVoiceChatPlugin.removeFromVoiceChat(player.getUuid());
        this.sync();
    }

    /**
     * Release all swallowed players at the given position
     */
    public void releaseAllPlayers(Vec3d position) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        for (UUID uuid : new ArrayList<>(swallowedPlayers)) {
            PlayerEntity swallowed = serverWorld.getPlayerByUuid(uuid);
            if (swallowed != null) {
                SwallowedPlayerComponent comp = SwallowedPlayerComponent.KEY.get(swallowed);
                comp.release(position);
                NoellesrolesVoiceChatPlugin.removeFromVoiceChat(uuid);
            }
        }

        swallowedPlayers.clear();
        NoellesrolesVoiceChatPlugin.clearStomachGroup(player.getUuid());
        this.sync();
    }

    /**
     * Check if Taotie has swallowed all other players (win condition 1)
     * Win if: all other alive players are in Taotie's stomach, or only Taotie is alive
     */
    public boolean hasSwallowedEveryone() {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return false;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(serverWorld);

        // Count alive players not in stomach (excluding Taotie)
        int aliveNotSwallowedCount = 0;
        for (UUID uuid : gameWorldComponent.getAllPlayers()) {
            if (uuid.equals(player.getUuid())) continue; // Skip Taotie

            PlayerEntity p = serverWorld.getPlayerByUuid(uuid);
            if (GameFunctions.isPlayerAliveAndSurvival(p)) {
                // Check if this player is swallowed
                if (!swallowedPlayers.contains(uuid)) {
                    aliveNotSwallowedCount++;
                }
            }
        }

        // Win if no alive players outside stomach (all are swallowed or dead)
        return aliveNotSwallowedCount == 0 && !swallowedPlayers.isEmpty();
    }

    /**
     * Check if Taotie Moment has completed (win condition 2)
     */
    public boolean hasTaotieMomentCompleted() {
        return taotieMomentActive && taotieMomentTicks <= 0;
    }

    /**
     * Check and trigger Taotie Moment based on alive count
     */
    public void checkAndTriggerMoment(int aliveCount) {
        if (taotieMomentActive) return;
        if (triggerThreshold < 2) return;

        if (aliveCount <= triggerThreshold) {
            triggerTaotieMoment();
        }
    }

    /**
     * Trigger Taotie Moment
     */
    private void triggerTaotieMoment() {
        this.taotieMomentActive = true;
        this.taotieMomentTicks = TAOTIE_MOMENT_DURATION;

        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        // Broadcast to all players with Title
        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            // Send Title (主标题)
            p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
                Text.translatable("title.noellesroles.taotie_moment")
                    .formatted(Formatting.DARK_RED, Formatting.BOLD)
            ));

            // Send Subtitle (副标题) - optional
            p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
                Text.translatable("subtitle.noellesroles.taotie_moment")
                    .formatted(Formatting.RED)
            ));

            // Set Title times (fadeIn, stay, fadeOut in ticks)
            p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(
                10, 100, 10
            ));

            // Also send chat message
            p.sendMessage(Text.translatable("title.noellesroles.taotie_moment")
                    .formatted(Formatting.DARK_RED, Formatting.BOLD), false);
        }

        broadcastCountdown();
        this.sync();
    }

    /**
     * End Taotie Moment (called when Taotie dies)
     */
    public void endTaotieMoment() {
        if (!taotieMomentActive) return;

        this.taotieMomentActive = false;
        this.taotieMomentTicks = 0;

        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        // Broadcast end message to all players
        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            // Clear Title
            p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket(false));

            // Send end message in chat and actionbar
            p.sendMessage(Text.translatable("tip.noellesroles.taotie_moment_ended")
                    .formatted(Formatting.GRAY), true);
        }

        this.sync();
    }

    /**
     * Broadcast Taotie Moment countdown to all players
     */
    private void broadcastCountdown() {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return;

        int secondsLeft = taotieMomentTicks / 20;
        Text countdownText = Text.translatable("tip.noellesroles.taotie_moment_countdown", secondsLeft);

        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            p.sendMessage(countdownText, true); // ActionBar
        }
    }

    @Override
    public void serverTick() {
        // Decrease swallow cooldown
        if (swallowCooldown > 0) {
            swallowCooldown--;
            // Sync every second for HUD update
            if (swallowCooldown % 20 == 0) {
                this.sync();
            }
        }

        // Handle Taotie Moment
        if (taotieMomentActive && taotieMomentTicks > 0) {
            taotieMomentTicks--;

            // Broadcast countdown every second and sync
            if (taotieMomentTicks % 20 == 0) {
                broadcastCountdown();
                this.sync();
            }

            // If moment completed, win condition is checked in CheckWinCondition event
        }
    }

    // Getters and Setters
    public int getSwallowCooldown() {
        return swallowCooldown;
    }

    public void setSwallowCooldown(int cooldown) {
        this.swallowCooldown = cooldown;
        this.sync();
    }

    public int getSwallowedCount() {
        // On client side, use synced count; on server side, use actual list size
        return player.getWorld().isClient() ? swallowedCountForClient : swallowedPlayers.size();
    }

    public List<UUID> getSwallowedPlayers() {
        return new ArrayList<>(swallowedPlayers);
    }

    public boolean isTaotieMomentActive() {
        return taotieMomentActive;
    }

    public int getTaotieMomentTicks() {
        return taotieMomentTicks;
    }

    public int getTotalPlayersAtStart() {
        return totalPlayersAtStart;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("swallowCooldown", this.swallowCooldown);
        tag.putBoolean("taotieMomentActive", this.taotieMomentActive);
        tag.putInt("taotieMomentTicks", this.taotieMomentTicks);
        tag.putInt("triggerThreshold", this.triggerThreshold);
        tag.putInt("totalPlayersAtStart", this.totalPlayersAtStart);
        tag.putInt("calculatedSwallowCooldown", this.calculatedSwallowCooldown);

        NbtList swallowedList = new NbtList();
        for (UUID uuid : this.swallowedPlayers) {
            swallowedList.add(NbtString.of(uuid.toString()));
        }
        tag.put("swallowedPlayers", swallowedList);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.swallowCooldown = tag.contains("swallowCooldown") ? tag.getInt("swallowCooldown") : 0;
        this.taotieMomentActive = tag.getBoolean("taotieMomentActive");
        this.taotieMomentTicks = tag.getInt("taotieMomentTicks");
        this.triggerThreshold = tag.getInt("triggerThreshold");
        this.totalPlayersAtStart = tag.getInt("totalPlayersAtStart");
        this.calculatedSwallowCooldown = tag.contains("calculatedSwallowCooldown")
            ? tag.getInt("calculatedSwallowCooldown")
            : SWALLOW_COOLDOWN;

        this.swallowedPlayers.clear();
        if (tag.contains("swallowedPlayers")) {
            NbtList swallowedList = tag.getList("swallowedPlayers", NbtString.STRING_TYPE);
            for (int i = 0; i < swallowedList.size(); i++) {
                this.swallowedPlayers.add(UUID.fromString(swallowedList.getString(i)));
            }
        }
    }
}
