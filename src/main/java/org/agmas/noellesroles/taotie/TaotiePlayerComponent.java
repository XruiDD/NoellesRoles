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
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main component for Taotie player
 * Manages swallowed players, cooldowns, and Taotie Moment
 */
public class TaotiePlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<TaotiePlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "taotie"), TaotiePlayerComponent.class);

    // Constants
    public static final int SWALLOW_COOLDOWN = GameConstants.getInTicks(0, 45); // 45 seconds
    public static final int SWALLOW_DISTANCE_SQUARED = 9; // 3 blocks squared
    public static final int TAOTIE_MOMENT_DURATION = GameConstants.getInTicks(2, 0); // 2 minutes

    private final PlayerEntity player;

    // Swallowed players list
    private final List<UUID> swallowedPlayers = new ArrayList<>();

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
        int swallowedCount = buf.readInt();
        // We don't sync the actual UUIDs to client, just the count
        this.swallowedPlayers.clear();
        for (int i = 0; i < swallowedCount; i++) {
            this.swallowedPlayers.add(UUID.randomUUID()); // Placeholder for count
        }
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

        // Check if target is alive
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
        GameMode originalMode = target.interactionManager.getGameMode();
        targetSwallowed.setSwallowed(taotie.getUuid(), originalMode);
        swallowedPlayers.add(target.getUuid());

        // Set up voice chat group
        NoellesrolesVoiceChatPlugin.addToStomachGroup(taotie, target);

        // Set cooldown (使用动态计算的冷却时间)
        swallowCooldown = calculatedSwallowCooldown;

        // 检查是否是连环杀手的目标，如果是则触发目标切换
        if (target.getWorld() instanceof ServerWorld serverWorld) {
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

        this.sync();
        return true;
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
     */
    public boolean hasSwallowedEveryone() {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) return false;

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(serverWorld);

        // Count alive players (excluding Taotie and swallowed players)
        int aliveCount = 0;
        for (UUID uuid : gameWorldComponent.getAllPlayers()) {
            if (uuid.equals(player.getUuid())) continue; // Skip Taotie
            if (swallowedPlayers.contains(uuid)) continue; // Skip swallowed

            PlayerEntity p = serverWorld.getPlayerByUuid(uuid);
            if (GameFunctions.isPlayerAliveAndSurvival(p)) {
                aliveCount++;
            }
        }

        // Win if no one else is alive (all are swallowed or dead)
        return aliveCount == 0 && !swallowedPlayers.isEmpty();
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

        // Broadcast to all players
        for (ServerPlayerEntity p : serverWorld.getPlayers()) {
            // Title announcement
            p.sendMessage(Text.translatable("title.noellesroles.taotie_moment")
                    .formatted(Formatting.DARK_RED, Formatting.BOLD), false);
        }

        broadcastCountdown();
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

        // 病原体感染共享传播（在饕餮肚子内）
        if (!swallowedPlayers.isEmpty() && player.getWorld() instanceof ServerWorld serverWorld) {
            boolean anyoneInfected = false;
            UUID pathogenUuid = null;

            // 检查饕餮是否被感染
            InfectedPlayerComponent taotieInfected = InfectedPlayerComponent.KEY.get(player);
            if (taotieInfected.isInfected()) {
                anyoneInfected = true;
                pathogenUuid = taotieInfected.getInfectedBy();
            }

            // 检查被吞玩家是否有人被感染
            if (!anyoneInfected) {
                for (UUID uuid : swallowedPlayers) {
                    PlayerEntity swallowed = serverWorld.getPlayerByUuid(uuid);
                    if (swallowed != null) {
                        InfectedPlayerComponent infected = InfectedPlayerComponent.KEY.get(swallowed);
                        if (infected.isInfected()) {
                            anyoneInfected = true;
                            pathogenUuid = infected.getInfectedBy();
                            break;
                        }
                    }
                }
            }

            // 如果有人感染，传播给所有人
            if (anyoneInfected && pathogenUuid != null) {
                if (!taotieInfected.isInfected()) {
                    taotieInfected.setInfected(true, pathogenUuid);
                }
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
        }

        // Ensure swallowed players stay in spectator mode following Taotie
        // This is handled by SwallowedPlayerComponent
    }

    @Override
    public void clientTick() {
        // Client-side tick (for HUD updates)
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
        return swallowedPlayers.size();
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
