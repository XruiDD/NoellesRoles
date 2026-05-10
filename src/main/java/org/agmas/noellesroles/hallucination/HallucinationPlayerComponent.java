package org.agmas.noellesroles.hallucination;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.item.DoubleBarrelShotgunItem;
import org.agmas.noellesroles.item.PoisonNeedleItem;
import org.agmas.noellesroles.item.RiotShieldItem;
import org.agmas.noellesroles.packet.HallucinationDummyHitC2SPacket;
import org.agmas.noellesroles.packet.HallucinationDummyUseC2SPacket;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class HallucinationPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<HallucinationPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "hallucination"),
            HallucinationPlayerComponent.class
    );

    public static final int ROLL_INTERVAL_TICKS = GameConstants.getInTicks(0, 10);
    public static final int ELEVATED_EFFECT_GRACE_TICKS = GameConstants.getInTicks(0, 30);
    public static final int CLEANSE_IMMUNITY_TICKS = GameConstants.getInTicks(1, 0);
    public static final int SLEEP_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);
    public static final int SLEEP_RECOVERY_TICKS = GameConstants.SLEEP_TASK_DURATION;
    public static final int HIDDEN_BODIES_TICKS = GameConstants.getInTicks(1, 0);
    public static final int FAKE_UI_TICKS = GameConstants.getInTicks(3, 0);
    public static final int HIDDEN_PLAYER_TICKS = GameConstants.getInTicks(2, 0);
    public static final int INSTINCT_MISJUDGE_TICKS = GameConstants.getInTicks(2, 0);
    public static final int SKIN_SCRAMBLE_TICKS = GameConstants.getInTicks(3, 0);
    public static final int HELD_ITEM_SCRAMBLE_TICKS = GameConstants.getInTicks(2, 0);
    public static final int FAKE_SANITY_TICKS = GameConstants.getInTicks(3, 0);
    public static final int HIDDEN_UI_TICKS = GameConstants.getInTicks(3, 0);
    public static final int KILLER_DUMMY_BODY_TICKS = GameConstants.TIME_TO_DECOMPOSITION + GameConstants.DECOMPOSING_TIME;
    private static final double DUMMY_MELEE_DISTANCE_SQUARED = 16.0D;
    private static final double DUMMY_GUN_DISTANCE_SQUARED = 30.0D * 30.0D;

    private final PlayerEntity player;
    private final Map<HallucinationEffectId, HallucinationActiveEntry> activeEffects = new EnumMap<>(HallucinationEffectId.class);
    private final Map<HallucinationEffectId, Integer> pendingRemovalTicks = new EnumMap<>(HallucinationEffectId.class);
    private final Map<UUID, HallucinationDummyState> dummies = new HashMap<>();
    private final Map<UUID, Integer> fakeBodies = new HashMap<>();
    private final Map<UUID, Identifier> fakeBodyDeathReasons = new HashMap<>();
    private final Map<UUID, HallucinationDummyRewardReference> killerDummyRewards = new HashMap<>();
    private final Map<UUID, HallucinationDummyBombState> dummyBombStates = new HashMap<>();
    private final Map<UUID, HallucinationDummyPoisonState> dummyPoisonStates = new HashMap<>();
    private final Map<UUID, Integer> pendingDummyRemovalTicks = new HashMap<>();
    private final Map<UUID, Integer> pendingFakeBodyRemovalTicks = new HashMap<>();
    private final EnumMap<HallucinationUiSlot, Integer> hiddenUiSlots = new EnumMap<>(HallucinationUiSlot.class);
    private final Map<UUID, Integer> hiddenPlayers = new HashMap<>();
    private final Map<UUID, Boolean> instinctMisjudges = new HashMap<>();
    private final Map<UUID, Integer> instinctMisjudgeTimers = new HashMap<>();
    private final List<HallucinationEffectId> lastAppliedOrder = new ArrayList<>();

    private int rollCooldownTicks;
    private int hallucinationImmunityTicks;
    private int sleepCooldownTicks;
    private int sleepRecoveryTicks;
    private int fakeMoneyAmount;
    private int fakeTimeOffsetSeconds;
    private int fakeSanityPercent = -1;
    private int fakeTaskCount = -1;
    private int shopShuffleSeed = -1;
    private boolean sleepSessionHandled;

    public HallucinationPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public PlayerEntity player() {
        return this.player;
    }

    public void reset() {
        this.activeEffects.clear();
        this.pendingRemovalTicks.clear();
        this.dummies.clear();
        this.fakeBodies.clear();
        this.fakeBodyDeathReasons.clear();
        this.killerDummyRewards.clear();
        this.dummyBombStates.clear();
        this.dummyPoisonStates.clear();
        this.pendingDummyRemovalTicks.clear();
        this.pendingFakeBodyRemovalTicks.clear();
        this.hiddenUiSlots.clear();
        this.hiddenPlayers.clear();
        this.instinctMisjudges.clear();
        this.instinctMisjudgeTimers.clear();
        this.lastAppliedOrder.clear();
        this.rollCooldownTicks = 0;
        this.hallucinationImmunityTicks = 0;
        this.sleepCooldownTicks = 0;
        this.sleepRecoveryTicks = 0;
        this.fakeMoneyAmount = 0;
        this.fakeTimeOffsetSeconds = 0;
        this.fakeSanityPercent = -1;
        this.fakeTaskCount = -1;
        resetShopShuffleSeed();
        this.sleepSessionHandled = false;
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    public int getRollCooldownTicks() {
        return this.rollCooldownTicks;
    }

    public void setRollCooldownTicks(int ticks) {
        this.rollCooldownTicks = Math.max(0, ticks);
    }

    public int getHallucinationImmunityTicks() {
        return this.hallucinationImmunityTicks;
    }

    public void grantCleanseImmunity() {
        this.hallucinationImmunityTicks = CLEANSE_IMMUNITY_TICKS;
    }

    public int getSleepCooldownTicks() {
        return this.sleepCooldownTicks;
    }

    public void startSleepCooldown() {
        this.sleepCooldownTicks = SLEEP_COOLDOWN_TICKS;
    }

    public int getSleepRecoveryTicks() {
        return this.sleepRecoveryTicks;
    }

    public void resetSleepRecoveryProgress() {
        this.sleepRecoveryTicks = 0;
    }

    public boolean tickSleepRecoveryProgress() {
        this.sleepRecoveryTicks = Math.min(SLEEP_RECOVERY_TICKS, this.sleepRecoveryTicks + 1);
        return this.sleepRecoveryTicks >= SLEEP_RECOVERY_TICKS;
    }

    public int getFakeMoneyAmount() {
        return this.fakeMoneyAmount;
    }

    public boolean hasFakeMoney() {
        return this.fakeMoneyAmount > 0;
    }

    public int getFakeTimeOffsetSeconds() {
        return this.fakeTimeOffsetSeconds;
    }

    public boolean hasFakeTimeOffset() {
        return this.fakeTimeOffsetSeconds != 0;
    }

    public int getFakeSanityPercent() {
        return this.fakeSanityPercent;
    }

    public int getFakeTaskCount() {
        return this.fakeTaskCount;
    }

    public int getShopShuffleSeed() {
        return this.shopShuffleSeed;
    }

    public int ensureShopShuffleSeed() {
        if (this.shopShuffleSeed < 0
                && this.player instanceof ServerPlayerEntity
                && this.player.getWorld() != null
                && !this.player.getWorld().isClient) {
            this.shopShuffleSeed = this.player.getWorld().getRandom().nextInt();
        }
        return this.shopShuffleSeed;
    }

    public void resetShopShuffleSeed() {
        this.shopShuffleSeed = -1;
    }

    public Set<UUID> getHiddenPlayerUuids() {
        return Set.copyOf(this.hiddenPlayers.keySet());
    }

    public boolean hidesPlayer(UUID playerUuid) {
        return playerUuid != null && this.hiddenPlayers.containsKey(playerUuid);
    }

    public boolean hasInstinctMisjudge(UUID playerUuid) {
        return playerUuid != null && this.instinctMisjudges.containsKey(playerUuid);
    }

    public boolean isInstinctMisjudgeTreatAsAlly(UUID playerUuid) {
        return playerUuid != null && Boolean.TRUE.equals(this.instinctMisjudges.get(playerUuid));
    }

    public Set<UUID> getInstinctMisjudgeTargets() {
        return Set.copyOf(this.instinctMisjudges.keySet());
    }

    public boolean isSleepSessionHandled() {
        return this.sleepSessionHandled;
    }

    public void setSleepSessionHandled(boolean sleepSessionHandled) {
        this.sleepSessionHandled = sleepSessionHandled;
    }

    public boolean hasAnyHallucination() {
        return !this.activeEffects.isEmpty()
                || !this.dummies.isEmpty()
                || !this.fakeBodies.isEmpty()
                || !this.hiddenUiSlots.isEmpty()
                || !this.hiddenPlayers.isEmpty()
                || !this.instinctMisjudges.isEmpty();
    }

    public boolean hasEffect(HallucinationEffectId effectId) {
        return this.activeEffects.containsKey(effectId);
    }

    public Optional<HallucinationActiveEntry> getActiveEntry(HallucinationEffectId effectId) {
        return Optional.ofNullable(this.activeEffects.get(effectId));
    }

    public List<HallucinationActiveEntry> getActiveEntries() {
        return this.activeEffects.values().stream().toList();
    }

    public List<HallucinationDummyState> getDummyStates() {
        return this.dummies.values().stream().toList();
    }

    public boolean hasFakeBody(UUID bodyUuid) {
        return bodyUuid != null && this.fakeBodies.containsKey(bodyUuid);
    }

    public List<UUID> getFakeBodyIds() {
        return List.copyOf(this.fakeBodies.keySet());
    }

    public Identifier getFakeBodyDeathReason(UUID bodyUuid) {
        return this.fakeBodyDeathReasons.getOrDefault(bodyUuid, GameConstants.DeathReasons.KNIFE);
    }

    public Map<UUID, Identifier> getFakeBodyDeathReasons() {
        return Map.copyOf(this.fakeBodyDeathReasons);
    }

    public boolean isUiHidden(HallucinationUiSlot slot) {
        return this.hiddenUiSlots.containsKey(slot);
    }

    public EnumSet<HallucinationUiSlot> getHiddenUiSlots() {
        return this.hiddenUiSlots.isEmpty()
                ? EnumSet.noneOf(HallucinationUiSlot.class)
                : EnumSet.copyOf(this.hiddenUiSlots.keySet());
    }

    public boolean canRollNewHallucination() {
        return this.rollCooldownTicks <= 0 && this.hallucinationImmunityTicks <= 0;
    }

    public void markRollChecked() {
        this.rollCooldownTicks = ROLL_INTERVAL_TICKS;
    }

    public boolean canApply(HallucinationEffectId effectId) {
        if (effectId == null) {
            return false;
        }
        if (!effectId.isRepeatable() && this.activeEffects.containsKey(effectId)) {
            return false;
        }
        if (effectId == HallucinationEffectId.HIDDEN_UI && this.hiddenUiSlots.size() >= HallucinationUiSlot.values().length) {
            return false;
        }
        return true;
    }

    public boolean hasHiddenPlayerTarget(UUID targetUuid) {
        return targetUuid != null && this.hiddenPlayers.containsKey(targetUuid);
    }

    public boolean hasInstinctMisjudgeTarget(UUID targetUuid) {
        return targetUuid != null && this.instinctMisjudges.containsKey(targetUuid);
    }

    public void addOrRefresh(HallucinationEffectId effectId, int durationTicks) {
        addOrRefresh(effectId, durationTicks, 0, HallucinationTargetKind.NONE, null, null);
    }

    public void addOrRefresh(HallucinationEffectId effectId,
                             int durationTicks,
                             int stackDelta,
                             HallucinationTargetKind targetKind,
                             UUID targetUuid,
                             HallucinationUiSlot uiSlot) {
        HallucinationActiveEntry current = this.activeEffects.get(effectId);
        int nextDuration = Math.max(durationTicks, current != null ? current.remainingTicks() : 0);
        int nextStack = stackDelta;
        if (current != null) {
            nextStack += current.stackValue();
        }
        HallucinationActiveEntry updated = new HallucinationActiveEntry(
                effectId,
                nextDuration,
                nextStack,
                targetKind == null ? HallucinationTargetKind.NONE : targetKind,
                targetUuid,
                uiSlot
        );
        this.activeEffects.put(effectId, updated);
        this.pendingRemovalTicks.remove(effectId);
        this.lastAppliedOrder.remove(effectId);
        this.lastAppliedOrder.add(effectId);
    }

    public boolean removeEffect(HallucinationEffectId effectId) {
        HallucinationActiveEntry removed = this.activeEffects.remove(effectId);
        this.pendingRemovalTicks.remove(effectId);
        this.lastAppliedOrder.remove(effectId);
        if (removed == null) {
            return false;
        }

        if (effectId == HallucinationEffectId.FAKE_TIME) {
            this.fakeTimeOffsetSeconds = 0;
        } else if (effectId == HallucinationEffectId.FAKE_MONEY) {
            this.fakeMoneyAmount = 0;
        } else if (effectId == HallucinationEffectId.FAKE_SANITY) {
            this.fakeSanityPercent = -1;
            this.fakeTaskCount = -1;
        } else if (effectId == HallucinationEffectId.HIDDEN_UI) {
            this.hiddenUiSlots.clear();
        } else if (effectId == HallucinationEffectId.HIDDEN_PLAYER) {
            this.hiddenPlayers.clear();
        } else if (effectId == HallucinationEffectId.INSTINCT_MISJUDGE) {
            this.instinctMisjudges.clear();
            this.instinctMisjudgeTimers.clear();
        }
        return true;
    }

    public boolean clearAllHallucinations(boolean grantImmunity) {
        boolean hadAny = hasAnyHallucination();
        this.activeEffects.keySet().stream().toList().forEach(this::removeEffect);
        this.dummies.clear();
        this.fakeBodies.clear();
        this.fakeBodyDeathReasons.clear();
        this.killerDummyRewards.clear();
        this.dummyBombStates.clear();
        this.dummyPoisonStates.clear();
        this.pendingDummyRemovalTicks.clear();
        this.pendingFakeBodyRemovalTicks.clear();
        this.hiddenUiSlots.clear();
        this.hiddenPlayers.clear();
        this.instinctMisjudges.clear();
        this.instinctMisjudgeTimers.clear();
        this.fakeMoneyAmount = 0;
        this.fakeTimeOffsetSeconds = 0;
        this.fakeSanityPercent = -1;
        this.fakeTaskCount = -1;
        this.shopShuffleSeed = -1;
        this.sleepRecoveryTicks = 0;
        this.sleepSessionHandled = false;
        this.pendingRemovalTicks.clear();
        this.lastAppliedOrder.clear();
        if (grantImmunity && hadAny) {
            grantCleanseImmunity();
        }
        if (hadAny && this.player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.translatable("hallucination.cleanse.success"), true);
        }
        return hadAny;
    }

    public void addFakeMoney(int amount) {
        if (amount <= 0 || this.player == null || this.player.getWorld() == null) {
            return;
        }
        if (!canSeeMoneyHud()) {
            return;
        }
        this.fakeMoneyAmount += amount;
        addOrRefresh(HallucinationEffectId.FAKE_MONEY, FAKE_UI_TICKS, amount, HallucinationTargetKind.NONE, null, null);
    }

    public void addFakeTimeSeconds(int seconds) {
        if (seconds <= 0 || this.player == null || this.player.getWorld() == null) {
            return;
        }
        if (!canSeeDefaultTimeHud()) {
            return;
        }
        this.fakeTimeOffsetSeconds += seconds;
        addOrRefresh(HallucinationEffectId.FAKE_TIME, FAKE_UI_TICKS, seconds, HallucinationTargetKind.NONE, null, null);
    }

    public void setFakeSanitySnapshot(int sanityPercent, int taskCount) {
        this.fakeSanityPercent = MathHelper.clamp(sanityPercent, 0, 100);
        this.fakeTaskCount = Math.max(0, taskCount);
        addOrRefresh(HallucinationEffectId.FAKE_SANITY, FAKE_SANITY_TICKS, 0, HallucinationTargetKind.NONE, null, null);
    }

    public void hideUi(HallucinationUiSlot slot) {
        if (slot == null) {
            return;
        }
        this.hiddenUiSlots.put(slot, HIDDEN_UI_TICKS);
        int stackCount = this.hiddenUiSlots.size();
        HallucinationActiveEntry current = this.activeEffects.get(HallucinationEffectId.HIDDEN_UI);
        int nextDuration = Math.max(HIDDEN_UI_TICKS, current != null ? current.remainingTicks() : 0);
        HallucinationActiveEntry updated = new HallucinationActiveEntry(
                HallucinationEffectId.HIDDEN_UI,
                nextDuration,
                stackCount,
                HallucinationTargetKind.NONE,
                null,
                null
        );
        this.activeEffects.put(HallucinationEffectId.HIDDEN_UI, updated);
        this.pendingRemovalTicks.remove(HallucinationEffectId.HIDDEN_UI);
        this.lastAppliedOrder.remove(HallucinationEffectId.HIDDEN_UI);
        this.lastAppliedOrder.add(HallucinationEffectId.HIDDEN_UI);
    }

    public void setHiddenPlayer(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }
        this.hiddenPlayers.put(playerUuid, HIDDEN_PLAYER_TICKS);
        refreshMultiTargetEffect(HallucinationEffectId.HIDDEN_PLAYER, this.hiddenPlayers.size(), HIDDEN_PLAYER_TICKS);
    }

    public void setInstinctMisjudge(UUID playerUuid, boolean treatAsAlly) {
        if (playerUuid == null) {
            return;
        }
        this.instinctMisjudges.put(playerUuid, treatAsAlly);
        this.instinctMisjudgeTimers.put(playerUuid, INSTINCT_MISJUDGE_TICKS);
        refreshMultiTargetEffect(HallucinationEffectId.INSTINCT_MISJUDGE, this.instinctMisjudges.size(), INSTINCT_MISJUDGE_TICKS);
    }

    public UUID addDummy(HallucinationDummyState state, UUID rewardReferenceVictimUuid) {
        this.dummies.put(state.id(), state);
        this.pendingDummyRemovalTicks.remove(state.id());
        if (state.kind() == HallucinationDummyKind.KILLER && rewardReferenceVictimUuid != null) {
            this.killerDummyRewards.put(state.id(), new HallucinationDummyRewardReference(rewardReferenceVictimUuid));
        }
        return state.id();
    }

    public boolean removeDummy(UUID dummyId) {
        this.killerDummyRewards.remove(dummyId);
        this.dummyBombStates.remove(dummyId);
        this.dummyPoisonStates.remove(dummyId);
        this.pendingDummyRemovalTicks.remove(dummyId);
        return this.dummies.remove(dummyId) != null;
    }

    public Optional<HallucinationDummyState> getDummy(UUID dummyId) {
        return Optional.ofNullable(this.dummies.get(dummyId));
    }

    public void addFakeBody(UUID bodyUuid, int durationTicks) {
        addFakeBody(bodyUuid, durationTicks, GameConstants.DeathReasons.KNIFE);
    }

    public void addFakeBody(UUID bodyUuid, int durationTicks, Identifier deathReason) {
        if (bodyUuid == null) {
            return;
        }
        this.fakeBodies.put(bodyUuid, Math.max(1, durationTicks));
        this.fakeBodyDeathReasons.put(bodyUuid, deathReason == null ? GameConstants.DeathReasons.KNIFE : deathReason);
        this.pendingFakeBodyRemovalTicks.remove(bodyUuid);
    }

    public Optional<HallucinationDummyState> findDummyByLocalEntityId(int entityId) {
        return this.dummies.values().stream()
                .filter(dummy -> dummy.localEntityId() == entityId)
                .findFirst();
    }

    public boolean hasBombOnDummy(UUID dummyId) {
        return dummyId != null && this.dummyBombStates.containsKey(dummyId);
    }

    public Optional<HallucinationDummyBombState> getDummyBombState(UUID dummyId) {
        return Optional.ofNullable(this.dummyBombStates.get(dummyId));
    }

    public Optional<HallucinationDummyPoisonState> getDummyPoisonState(UUID dummyId) {
        return Optional.ofNullable(this.dummyPoisonStates.get(dummyId));
    }

    public boolean placeBombOnDummy(UUID dummyId, PlayerEntity bomber) {
        if (dummyId == null || bomber == null || !this.dummies.containsKey(dummyId) || this.dummyBombStates.containsKey(dummyId)) {
            return false;
        }
        this.dummyBombStates.put(dummyId, new HallucinationDummyBombState(
                BomberPlayerComponent.BOMB_DELAY_TICKS,
                0,
                false,
                bomber.getUuid()
        ));
        BomberPlayerComponent.KEY.get(bomber).removeBombItemOnly();
        if (bomber.getWorld() != null
                && dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(bomber.getWorld()).isRole(bomber, WatheRoles.LOOSE_END)) {
            bomber.getItemCooldownManager().set(ModItems.TIMED_BOMB, 20 * 30);
        }
        bomber.getInventory().markDirty();
        if (bomber instanceof ServerPlayerEntity serverBomber) {
            serverBomber.playerScreenHandler.sendContentUpdates();
        }
        return true;
    }

    public boolean transferBombToDummy(UUID dummyId, PlayerEntity carrier) {
        if (dummyId == null || carrier == null || !this.dummies.containsKey(dummyId) || this.dummyBombStates.containsKey(dummyId)) {
            return false;
        }
        BomberPlayerComponent carrierComponent = BomberPlayerComponent.KEY.get(carrier);
        if (!carrierComponent.hasBomb() || !carrierComponent.isBeeping() || carrier.getItemCooldownManager().isCoolingDown(ModItems.TIMED_BOMB)) {
            return false;
        }
        this.dummyBombStates.put(dummyId, new HallucinationDummyBombState(
                0,
                carrierComponent.getBeepTimer(),
                true,
                carrierComponent.getBomberUuid()
        ));
        // The dummy is hallucination-only: remove the visible item, but keep the real carrier bomb ticking.
        carrierComponent.removeBombItemOnly();
        carrier.getItemCooldownManager().set(ModItems.TIMED_BOMB, BomberPlayerComponent.TRANSFER_COOLDOWN_TICKS);
        carrier.getInventory().markDirty();
        if (carrier instanceof ServerPlayerEntity serverCarrier) {
            serverCarrier.playerScreenHandler.sendContentUpdates();
        }
        return true;
    }

    public boolean applyPoisonToDummy(UUID dummyId, int poisonTicks, UUID poisonerUuid, Identifier poisonSource) {
        if (dummyId == null || !this.dummies.containsKey(dummyId) || poisonTicks <= 0) {
            return false;
        }
        HallucinationDummyPoisonState current = this.dummyPoisonStates.get(dummyId);
        int nextTicks = poisonTicks;
        if (current != null && current.poisonTicks() > 0) {
            nextTicks = Math.max(1, current.poisonTicks() - poisonTicks);
        }
        this.dummyPoisonStates.put(dummyId, new HallucinationDummyPoisonState(nextTicks, poisonerUuid, poisonSource));
        if (poisonSource == Noellesroles.POISON_SOURCE_NEEDLE) {
            this.player.getItemCooldownManager().set(ModItems.POISON_NEEDLE, PoisonNeedleItem.USE_COOLDOWN_TICKS);
        }
        return true;
    }

    public boolean catalyzeDummyPoison(UUID dummyId, UUID poisonerUuid) {
        HallucinationDummyPoisonState current = this.dummyPoisonStates.get(dummyId);
        if (current == null || current.poisonTicks() <= 0) {
            return false;
        }
        this.dummyPoisonStates.put(dummyId, new HallucinationDummyPoisonState(20 * 5, poisonerUuid, current.poisonSource()));
        this.player.getItemCooldownManager().remove(ModItems.POISON_NEEDLE);
        this.player.getMainHandStack().decrementUnlessCreative(1, this.player);
        this.player.getInventory().markDirty();
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.playerScreenHandler.sendContentUpdates();
        }
        return true;
    }

    public boolean shoveDummy(UUID dummyId, PlayerEntity attacker, double strength) {
        if (dummyId == null || attacker == null || strength <= 0 || !this.dummies.containsKey(dummyId)) {
            return false;
        }
        HallucinationDummyState current = this.dummies.get(dummyId);
        if (current == null) {
            return false;
        }

        Vec3d direction = current.position().subtract(attacker.getPos());
        Vec3d horizontal = new Vec3d(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSquared() < 1.0E-4D) {
            Vec3d rotation = attacker.getRotationVec(1.0F);
            horizontal = new Vec3d(rotation.x, 0.0D, rotation.z);
        }
        horizontal = horizontal.normalize().multiply(strength);
        Vec3d moved = current.position().add(horizontal);
        this.dummies.put(dummyId, new HallucinationDummyState(
                current.id(),
                current.kind(),
                current.skinUuid(),
                current.skinName(),
                moved,
                current.collidable(),
                current.localEntityId(),
                current.bodyYaw()
        ));
        return true;
    }

    public boolean cureDummyPoison(UUID dummyId) {
        return dummyId != null && this.dummyPoisonStates.remove(dummyId) != null;
    }

    public void tickServerState() {
        boolean dirty = false;

        if (hasAnyHallucination() && this.shopShuffleSeed == -1) {
            if (ensureShopShuffleSeed() != -1) {
                dirty = true;
            }
        }

        if (this.rollCooldownTicks > 0) {
            this.rollCooldownTicks--;
        }
        if (this.hallucinationImmunityTicks > 0) {
            this.hallucinationImmunityTicks--;
        }
        if (this.sleepCooldownTicks > 0) {
            this.sleepCooldownTicks--;
        }

        List<HallucinationEffectId> expired = new ArrayList<>();
        for (HallucinationActiveEntry entry : this.activeEffects.values()) {
            if (!entry.effectId().isDurationBased()) {
                continue;
            }
            int nextTicks = Math.max(0, entry.remainingTicks() - 1);
            if (nextTicks == 0) {
                expired.add(entry.effectId());
                dirty = true;
            } else if (nextTicks != entry.remainingTicks()) {
                this.activeEffects.put(entry.effectId(), entry.withRemainingTicks(nextTicks));
                dirty = true;
            }
        }
        expired.forEach(this::removeEffect);

        List<HallucinationEffectId> unapply = new ArrayList<>();
        for (Map.Entry<HallucinationEffectId, Integer> entry : this.pendingRemovalTicks.entrySet()) {
            int next = Math.max(0, entry.getValue() - 1);
            if (next == 0) {
                unapply.add(entry.getKey());
                dirty = true;
            } else {
                entry.setValue(next);
            }
        }
        unapply.forEach(effectId -> {
            this.pendingRemovalTicks.remove(effectId);
            removeEffect(effectId);
        });

        dirty |= tickUuidTimerMap(this.fakeBodies, this.fakeBodyDeathReasons);
        dirty |= tickArtifactTimerMap(this.pendingFakeBodyRemovalTicks, this.fakeBodies, this.fakeBodyDeathReasons);
        dirty |= tickArtifactTimerMap(this.pendingDummyRemovalTicks, this.dummies, this.killerDummyRewards);
        dirty |= tickDummyStatuses();
        dirty |= tickUuidTimerMap(this.hiddenPlayers);
        dirty |= tickUuidTimerMap(this.hiddenUiSlots);

        List<UUID> expiredMisjudges = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : this.instinctMisjudgeTimers.entrySet()) {
            int next = entry.getValue() - 1;
            if (next <= 0) {
                expiredMisjudges.add(entry.getKey());
            } else {
                entry.setValue(next);
            }
        }
        if (!expiredMisjudges.isEmpty()) {
            expiredMisjudges.forEach(uuid -> {
                this.instinctMisjudgeTimers.remove(uuid);
                this.instinctMisjudges.remove(uuid);
            });
            dirty = true;
        }

        refreshAggregatedEffectsFromCollections();

        if (!hasAnyHallucination() && this.shopShuffleSeed != -1) {
            resetShopShuffleSeed();
            dirty = true;
        }

        if (dirty) {
            sync();
        }
    }

    public void markHigherLevelForRemoval(int currentAllowedLevel) {
        for (HallucinationActiveEntry entry : this.activeEffects.values()) {
            if (entry.effectId().level() > currentAllowedLevel) {
                this.pendingRemovalTicks.put(entry.effectId(), ELEVATED_EFFECT_GRACE_TICKS);
            } else {
                this.pendingRemovalTicks.remove(entry.effectId());
            }
        }
        for (Map.Entry<UUID, HallucinationDummyState> entry : this.dummies.entrySet()) {
            if (entry.getValue().kind().hallucinationLevel() > currentAllowedLevel) {
                this.pendingDummyRemovalTicks.put(entry.getKey(), ELEVATED_EFFECT_GRACE_TICKS);
            } else {
                this.pendingDummyRemovalTicks.remove(entry.getKey());
            }
        }
        for (UUID bodyUuid : this.fakeBodies.keySet()) {
            if (currentAllowedLevel < HallucinationDummyKind.KILLER.hallucinationLevel()) {
                this.pendingFakeBodyRemovalTicks.put(bodyUuid, ELEVATED_EFFECT_GRACE_TICKS);
            } else {
                this.pendingFakeBodyRemovalTicks.remove(bodyUuid);
            }
        }
    }

    public boolean isPendingRemoval(HallucinationEffectId effectId) {
        return this.pendingRemovalTicks.containsKey(effectId);
    }

    public boolean handleDummyHit(HallucinationDummyHitC2SPacket payload) {
        if (payload == null || !canHitDummy(payload.dummyId(), payload.deathReason())) {
            return false;
        }
        return handleDummyRemoval(payload.dummyId(), new DummyDeathContext(payload.deathReason(), true, null, null, null));
    }

    public boolean handleDummyUse(HallucinationDummyUseC2SPacket payload) {
        if (payload == null) {
            return false;
        }
        UUID dummyId = payload.dummyId();
        return switch (payload.action()) {
            case MELEE_SHOVE -> canUseDummyMelee(dummyId)
                    && canShoveDummy()
                    && shoveDummy(dummyId, this.player, this.player.getMainHandStack().isOf(ModItems.RIOT_SHIELD) ? 0.75D : 0.45D);
            case POISON_NEEDLE_USE -> canUseDummyMelee(dummyId)
                    && isHoldingReady(ModItems.POISON_NEEDLE)
                    && applyPoisonToDummy(dummyId, 20 * 40, this.player.getUuid(), Noellesroles.POISON_SOURCE_NEEDLE);
            case CATALYST_USE -> canUseDummyMelee(dummyId)
                    && isHoldingReady(ModItems.CATALYST)
                    && GameWorldComponent.KEY.get(this.player.getWorld()).isRole(this.player, Noellesroles.POISONER)
                    && catalyzeDummyPoison(dummyId, this.player.getUuid());
            case ANTIDOTE_USE -> canUseDummyMelee(dummyId)
                    && isHoldingReady(ModItems.ANTIDOTE)
                    && getDummyPoisonState(dummyId).isPresent()
                    && cureDummyPoison(dummyId);
            case TIMED_BOMB_PLACE -> canUseDummyMelee(dummyId)
                    && isHoldingReady(ModItems.TIMED_BOMB)
                    && placeBombOnDummy(dummyId, this.player);
            case TIMED_BOMB_TRANSFER -> canUseDummyMelee(dummyId)
                    && isHoldingReady(ModItems.TIMED_BOMB)
                    && transferBombToDummy(dummyId, this.player);
        };
    }

    private boolean canHitDummy(UUID dummyId, Identifier deathReason) {
        Optional<HallucinationDummyState> dummy = getDummy(dummyId);
        if (dummy.isEmpty() || deathReason == null) {
            return false;
        }

        if (GameConstants.DeathReasons.KNIFE.equals(deathReason)) {
            return isDummyWithinDistance(dummy.get(), DUMMY_MELEE_DISTANCE_SQUARED)
                    && isHoldingReady(WatheItems.KNIFE);
        }
        if (GameConstants.DeathReasons.BAT.equals(deathReason)) {
            return isDummyWithinDistance(dummy.get(), DUMMY_MELEE_DISTANCE_SQUARED)
                    && isHoldingReady(WatheItems.BAT);
        }
        if (GameConstants.DeathReasons.GUN.equals(deathReason)) {
            ItemStack stack = this.player.getMainHandStack();
            if (stack.isOf(ModItems.DOUBLE_BARREL_SHOTGUN)) {
                double range = DoubleBarrelShotgunItem.RANGE;
                return isDummyWithinDistance(dummy.get(), range * range)
                        && !this.player.getItemCooldownManager().isCoolingDown(ModItems.DOUBLE_BARREL_SHOTGUN)
                        && DoubleBarrelShotgunItem.getLoadedShells(stack) > 0;
            }
            return isDummyWithinDistance(dummy.get(), DUMMY_GUN_DISTANCE_SQUARED)
                    && isHoldingReady(WatheItems.REVOLVER);
        }
        return false;
    }

    private boolean canUseDummyMelee(UUID dummyId) {
        return getDummy(dummyId)
                .map(dummy -> isDummyWithinDistance(dummy, DUMMY_MELEE_DISTANCE_SQUARED))
                .orElse(false);
    }

    private boolean canShoveDummy() {
        ItemStack stack = this.player.getMainHandStack();
        if (stack.isOf(ModItems.RIOT_SHIELD)) {
            return !this.player.getItemCooldownManager().isCoolingDown(ModItems.RIOT_SHIELD);
        }
        return stack.isOf(WatheItems.KNIFE) || stack.isOf(ModItems.POISON_NEEDLE);
    }

    private boolean isHoldingReady(Item item) {
        return this.player.getMainHandStack().isOf(item)
                && !this.player.getItemCooldownManager().isCoolingDown(item);
    }

    private boolean isDummyWithinDistance(HallucinationDummyState dummy, double maxDistanceSquared) {
        return dummy != null && this.player.squaredDistanceTo(dummy.position()) <= maxDistanceSquared;
    }

    public boolean handleDummyRemoval(UUID dummyId, DummyDeathContext context) {
        HallucinationDummyState dummy = this.dummies.remove(dummyId);
        HallucinationDummyRewardReference rewardReference = this.killerDummyRewards.remove(dummyId);
        this.dummyBombStates.remove(dummyId);
        this.dummyPoisonStates.remove(dummyId);
        if (dummy == null) {
            return false;
        }
        if (dummy.kind() == HallucinationDummyKind.KILLER) {
            addFakeBody(dummy.id(), KILLER_DUMMY_BODY_TICKS, context.deathReason());
            if (context.applyRewards()) {
                KillRewardResult reward = resolveDummyReward(rewardReference, context);
                if (reward.moneyDelta() > 0) {
                    addFakeMoney(reward.moneyDelta());
                }
                if (reward.timeDeltaSeconds() > 0) {
                    addFakeTimeSeconds(reward.timeDeltaSeconds());
                }
            }
        }
        applyDummyKillerCooldown(context.deathReason());
        sync();
        return true;
    }

    private KillRewardResult resolveDummyReward(HallucinationDummyRewardReference rewardReference, DummyDeathContext context) {
        if (rewardReference == null) {
            return KillRewardResult.NONE;
        }
        PlayerEntity victim = this.player.getWorld().getPlayerByUuid(rewardReference.victimUuid());
        return KillRewardResolver.resolve(new KillRewardContext(
                victim,
                this.player,
                true,
                context.deathReason(),
                context.bombOwnerUuid(),
                context.poisonerUuid(),
                context.poisonSource()
        ));
    }

    private void applyDummyKillerCooldown(Identifier deathReason) {
        if (this.player == null || deathReason == null) {
            return;
        }

        if (GameConstants.DeathReasons.KNIFE.equals(deathReason)) {
            Integer knifeCooldown = GameConstants.ITEM_COOLDOWNS.get(dev.doctor4t.wathe.index.WatheItems.KNIFE);
            if (knifeCooldown != null) {
                this.player.getItemCooldownManager().set(dev.doctor4t.wathe.index.WatheItems.KNIFE, knifeCooldown);
            }
            return;
        }

        if (GameConstants.DeathReasons.GUN.equals(deathReason)) {
            if (this.player.getMainHandStack().isOf(ModItems.DOUBLE_BARREL_SHOTGUN)) {
                int shells = DoubleBarrelShotgunItem.getLoadedShells(this.player.getMainHandStack());
                this.player.getItemCooldownManager().set(
                        ModItems.DOUBLE_BARREL_SHOTGUN,
                        shells <= 0 ? DoubleBarrelShotgunItem.EMPTY_COOLDOWN_TICKS : DoubleBarrelShotgunItem.FIRE_COOLDOWN_TICKS
                );
            } else {
                Integer gunCooldown = GameConstants.ITEM_COOLDOWNS.get(dev.doctor4t.wathe.index.WatheItems.REVOLVER);
                if (gunCooldown != null) {
                    this.player.getItemCooldownManager().set(dev.doctor4t.wathe.index.WatheItems.REVOLVER, gunCooldown);
                }
            }
            return;
        }

        if (Noellesroles.DEATH_REASON_THROWING_AXE.equals(deathReason) && this.player.getWorld() != null) {
            if (dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(this.player.getWorld()).isRole(this.player, WatheRoles.LOOSE_END)) {
                this.player.getItemCooldownManager().set(ModItems.THROWING_AXE, 20 * 30);
            }
            return;
        }

        if (GameConstants.DeathReasons.BAT.equals(deathReason)) {
            this.player.getItemCooldownManager().set(ModItems.RIOT_SHIELD, RiotShieldItem.SHOVE_COOLDOWN_TICKS);
        }
    }

    private void refreshMultiTargetEffect(HallucinationEffectId effectId, int stackCount, int durationTicks) {
        if (stackCount <= 0) {
            removeEffect(effectId);
            return;
        }
        HallucinationActiveEntry current = this.activeEffects.get(effectId);
        int nextDuration = Math.max(durationTicks, current != null ? current.remainingTicks() : 0);
        HallucinationActiveEntry updated = new HallucinationActiveEntry(
                effectId,
                nextDuration,
                stackCount,
                HallucinationTargetKind.NONE,
                null,
                null
        );
        this.activeEffects.put(effectId, updated);
        this.pendingRemovalTicks.remove(effectId);
        this.lastAppliedOrder.remove(effectId);
        this.lastAppliedOrder.add(effectId);
    }

    private void refreshAggregatedEffectsFromCollections() {
        if (this.hiddenUiSlots.isEmpty()) {
            removeEffect(HallucinationEffectId.HIDDEN_UI);
        } else if (this.activeEffects.containsKey(HallucinationEffectId.HIDDEN_UI)) {
            HallucinationActiveEntry current = this.activeEffects.get(HallucinationEffectId.HIDDEN_UI);
            this.activeEffects.put(HallucinationEffectId.HIDDEN_UI, current.withStackValue(this.hiddenUiSlots.size()));
        }

        if (this.hiddenPlayers.isEmpty()) {
            removeEffect(HallucinationEffectId.HIDDEN_PLAYER);
        } else if (this.activeEffects.containsKey(HallucinationEffectId.HIDDEN_PLAYER)) {
            HallucinationActiveEntry current = this.activeEffects.get(HallucinationEffectId.HIDDEN_PLAYER);
            this.activeEffects.put(HallucinationEffectId.HIDDEN_PLAYER, current.withStackValue(this.hiddenPlayers.size()));
        }

        if (this.instinctMisjudges.isEmpty()) {
            removeEffect(HallucinationEffectId.INSTINCT_MISJUDGE);
        } else if (this.activeEffects.containsKey(HallucinationEffectId.INSTINCT_MISJUDGE)) {
            HallucinationActiveEntry current = this.activeEffects.get(HallucinationEffectId.INSTINCT_MISJUDGE);
            this.activeEffects.put(HallucinationEffectId.INSTINCT_MISJUDGE, current.withStackValue(this.instinctMisjudges.size()));
        }
    }

    private boolean canSeeMoneyHud() {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        return gameWorld.canUseKillerFeatures(this.player) || gameWorld.isRole(this.player, Noellesroles.RECALLER);
    }

    private boolean canSeeDefaultTimeHud() {
        return GameWorldComponent.KEY.get(this.player.getWorld()).canUseKillerFeatures(this.player);
    }

    private static boolean tickUuidTimerMap(Map<UUID, Integer> map) {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : map.entrySet()) {
            int next = entry.getValue() - 1;
            if (next <= 0) {
                expired.add(entry.getKey());
            } else {
                entry.setValue(next);
            }
        }
        if (expired.isEmpty()) {
            return false;
        }
        expired.forEach(map::remove);
        return true;
    }

    private static boolean tickUuidTimerMap(Map<UUID, Integer> map, Map<UUID, ?> metadata) {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : map.entrySet()) {
            int next = entry.getValue() - 1;
            if (next <= 0) {
                expired.add(entry.getKey());
            } else {
                entry.setValue(next);
            }
        }
        if (expired.isEmpty()) {
            return false;
        }
        expired.forEach(uuid -> {
            map.remove(uuid);
            metadata.remove(uuid);
        });
        return true;
    }

    private static boolean tickArtifactTimerMap(Map<UUID, Integer> pendingMap, Map<UUID, Integer> artifacts) {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : pendingMap.entrySet()) {
            int next = entry.getValue() - 1;
            if (next <= 0) {
                expired.add(entry.getKey());
            } else {
                entry.setValue(next);
            }
        }
        if (expired.isEmpty()) {
            return false;
        }
        expired.forEach(uuid -> {
            pendingMap.remove(uuid);
            artifacts.remove(uuid);
        });
        return true;
    }

    private static boolean tickArtifactTimerMap(Map<UUID, Integer> pendingMap,
                                                Map<UUID, ?> artifacts,
                                                Map<UUID, ?> metadata) {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : pendingMap.entrySet()) {
            int next = entry.getValue() - 1;
            if (next <= 0) {
                expired.add(entry.getKey());
            } else {
                entry.setValue(next);
            }
        }
        if (expired.isEmpty()) {
            return false;
        }
        expired.forEach(uuid -> {
            pendingMap.remove(uuid);
            artifacts.remove(uuid);
            metadata.remove(uuid);
        });
        return true;
    }

    private static boolean tickUuidTimerMap(EnumMap<HallucinationUiSlot, Integer> map) {
        List<HallucinationUiSlot> expired = new ArrayList<>();
        for (Map.Entry<HallucinationUiSlot, Integer> entry : map.entrySet()) {
            int next = entry.getValue() - 1;
            if (next <= 0) {
                expired.add(entry.getKey());
            } else {
                entry.setValue(next);
            }
        }
        if (expired.isEmpty()) {
            return false;
        }
        expired.forEach(map::remove);
        return true;
    }

    private boolean tickDummyStatuses() {
        boolean dirty = false;
        List<UUID> explodedDummies = new ArrayList<>();
        for (Map.Entry<UUID, HallucinationDummyBombState> entry : this.dummyBombStates.entrySet()) {
            HallucinationDummyBombState state = entry.getValue();
            if (!state.beeping()) {
                int nextBombTimer = state.bombTimer() - 1;
                if (nextBombTimer <= 0) {
                    entry.setValue(state.startBeeping(BomberPlayerComponent.BEEP_DURATION_TICKS));
                } else {
                    entry.setValue(state.withBombTimer(nextBombTimer));
                }
                dirty = true;
                continue;
            }
            int nextBeepTimer = state.beepTimer() - 1;
            if (nextBeepTimer <= 0) {
                explodedDummies.add(entry.getKey());
            } else {
                entry.setValue(state.withBeepTimer(nextBeepTimer));
            }
            dirty = true;
        }
        for (UUID dummyId : explodedDummies) {
            HallucinationDummyBombState state = this.dummyBombStates.remove(dummyId);
            handleDummyRemoval(dummyId, new DummyDeathContext(
                    Noellesroles.DEATH_REASON_BOMB,
                    true,
                    state != null ? state.bomberUuid() : null,
                    null,
                    null
            ));
            dirty = true;
        }

        List<UUID> poisonedOut = new ArrayList<>();
        for (Map.Entry<UUID, HallucinationDummyPoisonState> entry : this.dummyPoisonStates.entrySet()) {
            int nextPoisonTicks = entry.getValue().poisonTicks() - 1;
            if (nextPoisonTicks <= 0) {
                poisonedOut.add(entry.getKey());
            } else {
                entry.setValue(entry.getValue().withPoisonTicks(nextPoisonTicks));
            }
            dirty = true;
        }
        for (UUID dummyId : poisonedOut) {
            HallucinationDummyPoisonState state = this.dummyPoisonStates.remove(dummyId);
            handleDummyRemoval(dummyId, new DummyDeathContext(
                    GameConstants.DeathReasons.POISON,
                    true,
                    null,
                    state != null ? state.poisonerUuid() : null,
                    state != null ? state.poisonSource() : null
            ));
            dirty = true;
        }
        return dirty;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("rollCooldownTicks", this.rollCooldownTicks);
        tag.putInt("hallucinationImmunityTicks", this.hallucinationImmunityTicks);
        tag.putInt("sleepCooldownTicks", this.sleepCooldownTicks);
        tag.putInt("sleepRecoveryTicks", this.sleepRecoveryTicks);
        tag.putInt("fakeMoneyAmount", this.fakeMoneyAmount);
        tag.putInt("fakeTimeOffsetSeconds", this.fakeTimeOffsetSeconds);
        tag.putInt("fakeSanityPercent", this.fakeSanityPercent);
        tag.putInt("fakeTaskCount", this.fakeTaskCount);
        tag.putInt("shopShuffleSeed", this.shopShuffleSeed);
        tag.putBoolean("sleepSessionHandled", this.sleepSessionHandled);

        NbtList effectList = new NbtList();
        for (HallucinationActiveEntry entry : this.activeEffects.values()) {
            NbtCompound effectTag = new NbtCompound();
            effectTag.putString("id", entry.effectId().id());
            effectTag.putInt("remainingTicks", entry.remainingTicks());
            effectTag.putInt("stackValue", entry.stackValue());
            effectTag.putString("targetKind", entry.targetKind().name());
            if (entry.targetUuid() != null) {
                effectTag.putUuid("targetUuid", entry.targetUuid());
            }
            if (entry.uiSlot() != null) {
                effectTag.putString("uiSlot", entry.uiSlot().name());
            }
            effectList.add(effectTag);
        }
        tag.put("activeEffects", effectList);

        NbtList pendingList = new NbtList();
        for (Map.Entry<HallucinationEffectId, Integer> entry : this.pendingRemovalTicks.entrySet()) {
            NbtCompound pendingTag = new NbtCompound();
            pendingTag.putString("id", entry.getKey().id());
            pendingTag.putInt("ticks", entry.getValue());
            pendingList.add(pendingTag);
        }
        tag.put("pendingRemovalTicks", pendingList);

        NbtList dummyList = new NbtList();
        for (HallucinationDummyState dummy : this.dummies.values()) {
            NbtCompound dummyTag = new NbtCompound();
            dummyTag.putUuid("id", dummy.id());
            dummyTag.putString("kind", dummy.kind().name());
            dummyTag.putUuid("skinUuid", dummy.skinUuid());
            dummyTag.putString("skinName", dummy.skinName());
            dummyTag.putDouble("x", dummy.position().x);
            dummyTag.putDouble("y", dummy.position().y);
            dummyTag.putDouble("z", dummy.position().z);
            dummyTag.putBoolean("collidable", dummy.collidable());
            dummyTag.putInt("localEntityId", dummy.localEntityId());
            dummyTag.putFloat("bodyYaw", dummy.bodyYaw());
            HallucinationDummyRewardReference reward = this.killerDummyRewards.get(dummy.id());
            if (reward != null && reward.victimUuid() != null) {
                dummyTag.putUuid("rewardReferenceVictim", reward.victimUuid());
            }
            dummyList.add(dummyTag);
        }
        tag.put("dummies", dummyList);

        NbtList bodyList = new NbtList();
        for (Map.Entry<UUID, Integer> entry : this.fakeBodies.entrySet()) {
            NbtCompound bodyTag = new NbtCompound();
            bodyTag.putUuid("id", entry.getKey());
            bodyTag.putInt("ticks", entry.getValue());
            Identifier deathReason = this.fakeBodyDeathReasons.get(entry.getKey());
            if (deathReason != null) {
                bodyTag.putString("deathReason", deathReason.toString());
            }
            bodyList.add(bodyTag);
        }
        tag.put("fakeBodies", bodyList);

        NbtList pendingDummyList = new NbtList();
        for (Map.Entry<UUID, Integer> entry : this.pendingDummyRemovalTicks.entrySet()) {
            NbtCompound pendingTag = new NbtCompound();
            pendingTag.putUuid("id", entry.getKey());
            pendingTag.putInt("ticks", entry.getValue());
            pendingDummyList.add(pendingTag);
        }
        tag.put("pendingDummyRemovalTicks", pendingDummyList);

        NbtList pendingFakeBodyList = new NbtList();
        for (Map.Entry<UUID, Integer> entry : this.pendingFakeBodyRemovalTicks.entrySet()) {
            NbtCompound pendingTag = new NbtCompound();
            pendingTag.putUuid("id", entry.getKey());
            pendingTag.putInt("ticks", entry.getValue());
            pendingFakeBodyList.add(pendingTag);
        }
        tag.put("pendingFakeBodyRemovalTicks", pendingFakeBodyList);

        NbtList hiddenUiList = new NbtList();
        for (Map.Entry<HallucinationUiSlot, Integer> entry : this.hiddenUiSlots.entrySet()) {
            NbtCompound slotTag = new NbtCompound();
            slotTag.putString("slot", entry.getKey().name());
            slotTag.putInt("ticks", entry.getValue());
            hiddenUiList.add(slotTag);
        }
        tag.put("hiddenUiSlots", hiddenUiList);

        NbtList hiddenPlayersList = new NbtList();
        for (Map.Entry<UUID, Integer> entry : this.hiddenPlayers.entrySet()) {
            NbtCompound hiddenTag = new NbtCompound();
            hiddenTag.putUuid("id", entry.getKey());
            hiddenTag.putInt("ticks", entry.getValue());
            hiddenPlayersList.add(hiddenTag);
        }
        tag.put("hiddenPlayers", hiddenPlayersList);

        NbtList bombList = new NbtList();
        for (Map.Entry<UUID, HallucinationDummyBombState> entry : this.dummyBombStates.entrySet()) {
            NbtCompound bombTag = new NbtCompound();
            bombTag.putUuid("id", entry.getKey());
            bombTag.putInt("bombTimer", entry.getValue().bombTimer());
            bombTag.putInt("beepTimer", entry.getValue().beepTimer());
            bombTag.putBoolean("beeping", entry.getValue().beeping());
            if (entry.getValue().bomberUuid() != null) {
                bombTag.putUuid("bomberUuid", entry.getValue().bomberUuid());
            }
            bombList.add(bombTag);
        }
        tag.put("dummyBombStates", bombList);

        NbtList poisonList = new NbtList();
        for (Map.Entry<UUID, HallucinationDummyPoisonState> entry : this.dummyPoisonStates.entrySet()) {
            NbtCompound poisonTag = new NbtCompound();
            poisonTag.putUuid("id", entry.getKey());
            poisonTag.putInt("poisonTicks", entry.getValue().poisonTicks());
            if (entry.getValue().poisonerUuid() != null) {
                poisonTag.putUuid("poisonerUuid", entry.getValue().poisonerUuid());
            }
            if (entry.getValue().poisonSource() != null) {
                poisonTag.putString("poisonSource", entry.getValue().poisonSource().toString());
            }
            poisonList.add(poisonTag);
        }
        tag.put("dummyPoisonStates", poisonList);

        NbtList misjudgeList = new NbtList();
        for (Map.Entry<UUID, Boolean> entry : this.instinctMisjudges.entrySet()) {
            NbtCompound misjudgeTag = new NbtCompound();
            misjudgeTag.putUuid("id", entry.getKey());
            misjudgeTag.putBoolean("ally", entry.getValue());
            misjudgeTag.putInt("ticks", this.instinctMisjudgeTimers.getOrDefault(entry.getKey(), INSTINCT_MISJUDGE_TICKS));
            misjudgeList.add(misjudgeTag);
        }
        tag.put("instinctMisjudges", misjudgeList);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.rollCooldownTicks = tag.getInt("rollCooldownTicks");
        this.hallucinationImmunityTicks = tag.getInt("hallucinationImmunityTicks");
        this.sleepCooldownTicks = tag.getInt("sleepCooldownTicks");
        this.sleepRecoveryTicks = tag.getInt("sleepRecoveryTicks");
        this.fakeMoneyAmount = tag.getInt("fakeMoneyAmount");
        this.fakeTimeOffsetSeconds = tag.getInt("fakeTimeOffsetSeconds");
        this.fakeSanityPercent = tag.getInt("fakeSanityPercent");
        this.fakeTaskCount = tag.getInt("fakeTaskCount");
        this.shopShuffleSeed = tag.contains("shopShuffleSeed") ? tag.getInt("shopShuffleSeed") : -1;
        this.sleepSessionHandled = tag.getBoolean("sleepSessionHandled");

        this.activeEffects.clear();
        NbtList effectList = tag.getList("activeEffects", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : effectList) {
            NbtCompound effectTag = (NbtCompound) element;
            HallucinationEffectId.byId(effectTag.getString("id")).ifPresent(effectId -> {
                HallucinationTargetKind targetKind = HallucinationTargetKind.valueOf(
                        effectTag.contains("targetKind") ? effectTag.getString("targetKind") : HallucinationTargetKind.NONE.name()
                );
                UUID targetUuid = effectTag.containsUuid("targetUuid") ? effectTag.getUuid("targetUuid") : null;
                HallucinationUiSlot slot = effectTag.contains("uiSlot") ? HallucinationUiSlot.valueOf(effectTag.getString("uiSlot")) : null;
                this.activeEffects.put(effectId, new HallucinationActiveEntry(
                        effectId,
                        effectTag.getInt("remainingTicks"),
                        effectTag.getInt("stackValue"),
                        targetKind,
                        targetUuid,
                        slot
                ));
            });
        }

        this.pendingRemovalTicks.clear();
        NbtList pendingList = tag.getList("pendingRemovalTicks", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : pendingList) {
            NbtCompound pendingTag = (NbtCompound) element;
            HallucinationEffectId.byId(pendingTag.getString("id"))
                    .ifPresent(effectId -> this.pendingRemovalTicks.put(effectId, pendingTag.getInt("ticks")));
        }

        this.dummies.clear();
        this.killerDummyRewards.clear();
        this.pendingDummyRemovalTicks.clear();
        NbtList dummyList = tag.getList("dummies", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : dummyList) {
            NbtCompound dummyTag = (NbtCompound) element;
            UUID dummyId = dummyTag.getUuid("id");
            this.dummies.put(dummyId, new HallucinationDummyState(
                    dummyId,
                    HallucinationDummyKind.valueOf(dummyTag.getString("kind")),
                    dummyTag.getUuid("skinUuid"),
                    dummyTag.getString("skinName"),
                    new net.minecraft.util.math.Vec3d(dummyTag.getDouble("x"), dummyTag.getDouble("y"), dummyTag.getDouble("z")),
                    dummyTag.getBoolean("collidable"),
                    dummyTag.contains("localEntityId") ? dummyTag.getInt("localEntityId") : Integer.MAX_VALUE - Math.abs(dummyId.hashCode()),
                    dummyTag.contains("bodyYaw") ? dummyTag.getFloat("bodyYaw") : Math.floorMod(dummyId.hashCode(), 360)
            ));
            if (dummyTag.containsUuid("rewardReferenceVictim")) {
                this.killerDummyRewards.put(dummyId, new HallucinationDummyRewardReference(dummyTag.getUuid("rewardReferenceVictim")));
            }
        }

        this.fakeBodies.clear();
        this.fakeBodyDeathReasons.clear();
        this.pendingFakeBodyRemovalTicks.clear();
        NbtList bodyList = tag.getList("fakeBodies", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : bodyList) {
            NbtCompound bodyTag = (NbtCompound) element;
            UUID bodyId = bodyTag.getUuid("id");
            this.fakeBodies.put(bodyId, bodyTag.getInt("ticks"));
            if (bodyTag.contains("deathReason")) {
                Identifier deathReason = Identifier.tryParse(bodyTag.getString("deathReason"));
                if (deathReason != null) {
                    this.fakeBodyDeathReasons.put(bodyId, deathReason);
                }
            }
        }

        NbtList pendingDummyList = tag.getList("pendingDummyRemovalTicks", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : pendingDummyList) {
            NbtCompound pendingTag = (NbtCompound) element;
            this.pendingDummyRemovalTicks.put(pendingTag.getUuid("id"), pendingTag.getInt("ticks"));
        }

        NbtList pendingFakeBodyList = tag.getList("pendingFakeBodyRemovalTicks", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : pendingFakeBodyList) {
            NbtCompound pendingTag = (NbtCompound) element;
            this.pendingFakeBodyRemovalTicks.put(pendingTag.getUuid("id"), pendingTag.getInt("ticks"));
        }

        this.hiddenUiSlots.clear();
        NbtList hiddenUiList = tag.getList("hiddenUiSlots", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : hiddenUiList) {
            NbtCompound slotTag = (NbtCompound) element;
            this.hiddenUiSlots.put(HallucinationUiSlot.valueOf(slotTag.getString("slot")), slotTag.getInt("ticks"));
        }

        this.hiddenPlayers.clear();
        NbtList hiddenPlayersList = tag.getList("hiddenPlayers", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : hiddenPlayersList) {
            NbtCompound hiddenTag = (NbtCompound) element;
            this.hiddenPlayers.put(hiddenTag.getUuid("id"), hiddenTag.getInt("ticks"));
        }

        this.dummyBombStates.clear();
        NbtList bombList = tag.getList("dummyBombStates", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : bombList) {
            NbtCompound bombTag = (NbtCompound) element;
            this.dummyBombStates.put(
                    bombTag.getUuid("id"),
                    new HallucinationDummyBombState(
                            bombTag.getInt("bombTimer"),
                            bombTag.getInt("beepTimer"),
                            bombTag.getBoolean("beeping"),
                            bombTag.containsUuid("bomberUuid") ? bombTag.getUuid("bomberUuid") : null
                    )
            );
        }

        this.dummyPoisonStates.clear();
        NbtList poisonList = tag.getList("dummyPoisonStates", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : poisonList) {
            NbtCompound poisonTag = (NbtCompound) element;
            this.dummyPoisonStates.put(
                    poisonTag.getUuid("id"),
                    new HallucinationDummyPoisonState(
                            poisonTag.getInt("poisonTicks"),
                            poisonTag.containsUuid("poisonerUuid") ? poisonTag.getUuid("poisonerUuid") : null,
                            poisonTag.contains("poisonSource") ? Identifier.tryParse(poisonTag.getString("poisonSource")) : null
                    )
            );
        }

        this.instinctMisjudges.clear();
        this.instinctMisjudgeTimers.clear();
        NbtList misjudgeList = tag.getList("instinctMisjudges", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : misjudgeList) {
            NbtCompound misjudgeTag = (NbtCompound) element;
            UUID uuid = misjudgeTag.getUuid("id");
            this.instinctMisjudges.put(uuid, misjudgeTag.getBoolean("ally"));
            this.instinctMisjudgeTimers.put(uuid, misjudgeTag.getInt("ticks"));
        }

        this.lastAppliedOrder.clear();
        this.lastAppliedOrder.addAll(this.activeEffects.keySet());
    }
}
