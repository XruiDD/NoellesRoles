package org.agmas.noellesroles.ferryman;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FerrymanPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<FerrymanPlayerComponent> KEY = ComponentRegistry.getOrCreate(
        Identifier.of(Noellesroles.MOD_ID, "ferryman"), FerrymanPlayerComponent.class
    );

    public static final int REACTION_WINDOW_TICKS = 8;
    public static final int REACTION_COOLDOWN_TICKS = GameConstants.getInTicks(0, 10);
    public static final int EMPTY_PENALTY_TICKS = GameConstants.getInTicks(0, 3);
    public static final int FERRY_COOLDOWN_TICKS = GameConstants.getInTicks(0, 3);
    public static final int COUNTER_STUN_TICKS = GameConstants.getInTicks(0, 6);
    public static final int COUNTER_SPEED_TICKS = GameConstants.getInTicks(0, 6);

    private final PlayerEntity player;
    private int ferriedCount = 0;
    private int ferriedRequired = 0;
    private int blessingStacks = 0;
    private int reactionTicks = 0;
    private UUID pendingAttackerUuid;
    private Identifier pendingDeathReason;
    private boolean won = false;
    private final List<UUID> ferriedBodies = new ArrayList<>();

    public FerrymanPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.ferriedCount = 0;
        this.ferriedRequired = 0;
        this.blessingStacks = 0;
        this.reactionTicks = 0;
        this.pendingAttackerUuid = null;
        this.pendingDeathReason = null;
        this.won = false;
        this.ferriedBodies.clear();
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
        buf.writeInt(this.ferriedCount);
        buf.writeInt(this.ferriedRequired);
        buf.writeInt(this.blessingStacks);
        buf.writeInt(this.reactionTicks);
        buf.writeBoolean(this.pendingAttackerUuid != null);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.ferriedCount = buf.readInt();
        this.ferriedRequired = buf.readInt();
        this.blessingStacks = buf.readInt();
        this.reactionTicks = buf.readInt();
        if (!buf.readBoolean()) {
            this.pendingAttackerUuid = null;
            this.pendingDeathReason = null;
        }
    }

    public int getFerriedCount() {
        return ferriedCount;
    }

    public int getFerriedRequired() {
        return ferriedRequired;
    }

    public void setFerriedRequired(int ferriedRequired) {
        this.ferriedRequired = Math.max(1, ferriedRequired);
        this.sync();
    }

    public int getBlessingStacks() {
        return blessingStacks;
    }

    public boolean hasWon() {
        return won;
    }

    public boolean hasFerriedBody(UUID bodyUuid) {
        return this.ferriedBodies.contains(bodyUuid);
    }

    public boolean addFerriedBody(UUID bodyUuid) {
        if (bodyUuid == null || this.ferriedBodies.contains(bodyUuid)) {
            return false;
        }
        this.ferriedBodies.add(bodyUuid);
        this.ferriedCount++;
        this.blessingStacks++;
        if (this.ferriedRequired > 0 && this.ferriedCount >= this.ferriedRequired) {
            this.won = true;
        }
        this.sync();
        return true;
    }

    public boolean beginReaction(UUID attackerUuid, Identifier deathReason) {
        if (this.reactionTicks > 0) {
            return false;
        }
        this.reactionTicks = REACTION_WINDOW_TICKS;
        this.pendingAttackerUuid = attackerUuid;
        this.pendingDeathReason = deathReason;
        this.sync();
        return true;
    }

    public boolean isReactionActive() {
        return this.reactionTicks > 0;
    }

    public Identifier getPendingDeathReason() {
        return pendingDeathReason;
    }

    public UUID getPendingAttackerUuid() {
        return pendingAttackerUuid;
    }

    public ReactionResult triggerReaction() {
        if (this.reactionTicks <= 0 || this.pendingDeathReason == null) {
            return ReactionResult.failure();
        }

        UUID attackerUuid = this.pendingAttackerUuid;
        Identifier deathReason = this.pendingDeathReason;
        boolean empowered = this.blessingStacks > 0;
        if (empowered) {
            this.blessingStacks--;
        }

        this.reactionTicks = 0;
        this.pendingAttackerUuid = null;
        this.pendingDeathReason = null;
        this.sync();
        return new ReactionResult(true, empowered, attackerUuid, deathReason);
    }

    public void clearReaction() {
        if (this.reactionTicks == 0 && this.pendingAttackerUuid == null && this.pendingDeathReason == null) {
            return;
        }
        this.reactionTicks = 0;
        this.pendingAttackerUuid = null;
        this.pendingDeathReason = null;
        this.sync();
    }

    public void applyCounterBlessing(ServerPlayerEntity attacker) {
        if (attacker != null) {
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, COUNTER_STUN_TICKS, 6, false, true, true));
            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, COUNTER_STUN_TICKS, 2, false, true, true));
            attacker.sendMessage(net.minecraft.text.Text.translatable("tip.ferryman.attacker_warn"), true);
        }

        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, COUNTER_SPEED_TICKS, 1, false, true, true));
        }
    }

    @Override
    public void serverTick() {
        if (this.reactionTicks > 0) {
            this.reactionTicks--;
            if (this.reactionTicks == 0) {
                this.sync();
            } else if (this.reactionTicks % 2 == 0) {
                this.sync();
            }
        }
    }

    @Override
    public void clientTick() {
        if (this.reactionTicks > 0) {
            this.reactionTicks--;
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("ferriedCount", this.ferriedCount);
        tag.putInt("ferriedRequired", this.ferriedRequired);
        tag.putInt("blessingStacks", this.blessingStacks);
        tag.putInt("reactionTicks", this.reactionTicks);
        tag.putBoolean("won", this.won);
        if (this.pendingAttackerUuid != null) {
            tag.putUuid("pendingAttackerUuid", this.pendingAttackerUuid);
        }
        if (this.pendingDeathReason != null) {
            tag.putString("pendingDeathReason", this.pendingDeathReason.toString());
        }

        NbtList ferriedList = new NbtList();
        for (UUID uuid : this.ferriedBodies) {
            ferriedList.add(NbtString.of(uuid.toString()));
        }
        tag.put("ferriedBodies", ferriedList);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.ferriedCount = tag.contains("ferriedCount") ? tag.getInt("ferriedCount") : 0;
        this.ferriedRequired = tag.contains("ferriedRequired") ? tag.getInt("ferriedRequired") : 0;
        this.blessingStacks = tag.contains("blessingStacks") ? tag.getInt("blessingStacks") : 0;
        this.reactionTicks = tag.contains("reactionTicks") ? tag.getInt("reactionTicks") : 0;
        this.won = tag.getBoolean("won");
        this.pendingAttackerUuid = tag.containsUuid("pendingAttackerUuid") ? tag.getUuid("pendingAttackerUuid") : null;
        this.pendingDeathReason = tag.contains("pendingDeathReason")
            ? Identifier.tryParse(tag.getString("pendingDeathReason"))
            : null;

        this.ferriedBodies.clear();
        if (tag.contains("ferriedBodies")) {
            NbtList ferriedList = tag.getList("ferriedBodies", NbtString.STRING_TYPE);
            for (int i = 0; i < ferriedList.size(); i++) {
                this.ferriedBodies.add(UUID.fromString(ferriedList.getString(i)));
            }
        }
    }

    public record ReactionResult(boolean success, boolean consumeBlessing, UUID attackerUuid, Identifier deathReason) {
        public static ReactionResult failure() {
            return new ReactionResult(false, false, null, null);
        }
    }
}
