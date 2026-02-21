package org.agmas.noellesroles.bartender;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.item.BaseSpiritItem;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;

public class BartenderPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<BartenderPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "bartender"), BartenderPlayerComponent.class);

    private final PlayerEntity player;
    public int glowTicks = 0;

    // 延迟调制品效果
    private int pendingEffectDelay = 0;
    private List<String> pendingIngredients = new ArrayList<>();

    // 威士忌护盾倒计时
    private int whiskeyShieldTicks = 0;

    // 伏特加亢奋标记：效果结束后清空体力
    private boolean vodkaStimulationActive = false;

    public void reset() {
        this.glowTicks = 0;
        this.pendingEffectDelay = 0;
        this.pendingIngredients.clear();
        this.whiskeyShieldTicks = 0;
        this.vodkaStimulationActive = false;
        this.sync();
    }

    public BartenderPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 设置延迟调制品效果
     */
    public void schedulePendingEffects(List<String> ingredients, int delayTicks) {
        this.pendingIngredients = new ArrayList<>(ingredients);
        this.pendingEffectDelay = delayTicks;
    }

    /**
     * 标记伏特加亢奋激活（效果结束后会清空体力 + 恢复 san）
     */
    public void setVodkaStimulationActive(boolean active) {
        this.vodkaStimulationActive = active;
    }

    /**
     * 启动威士忌护盾计时器
     */
    public void startWhiskeyShield(int durationTicks) {
        this.whiskeyShieldTicks = durationTicks;
    }

    public void serverTick() {
        if (this.glowTicks > 0) {
            --this.glowTicks;
            if (glowTicks == 0) {
                sync();
            }
        }

        // 延迟调制品效果倒计时
        if (this.pendingEffectDelay > 0) {
            --this.pendingEffectDelay;
            if (this.pendingEffectDelay == 0 && this.player instanceof ServerPlayerEntity serverPlayer) {
                BaseSpiritItem.applyIngredientEffectsStatic(serverPlayer, this.pendingIngredients);
                this.pendingIngredients.clear();
            }
        }

        // 伏特加亢奋结束检测：效果消失后清空体力
        if (this.vodkaStimulationActive && this.player instanceof ServerPlayerEntity serverPlayer) {
            if (!serverPlayer.hasStatusEffect(ModEffects.STIMULATION)) {
                // 亢奋效果结束，清空体力
                PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(serverPlayer);
                stamina.setSprintingTicks(0);
                stamina.setExhausted(true);
                // 3s 缓慢II
                serverPlayer.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.SLOWNESS, 3 * 20, 1, false, false, true));
                // 额外恢复 20% san
                PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(serverPlayer);
                moodComponent.setMood(Math.min(1.0f, moodComponent.getMood() + 0.2f));
                this.vodkaStimulationActive = false;
            }
        }

        // 威士忌护盾倒计时
        if (this.whiskeyShieldTicks > 0) {
            --this.whiskeyShieldTicks;
            if (this.whiskeyShieldTicks == 0 && this.player instanceof ServerPlayerEntity) {
                // 护盾时间到，移除铁人 buff
                org.agmas.noellesroles.professor.IronManPlayerComponent ironMan =
                        org.agmas.noellesroles.professor.IronManPlayerComponent.KEY.get(this.player);
                if (ironMan.hasBuff()) {
                    ironMan.removeBuff();
                }
            }
        }
    }

    public void startGlow() {
        setGlowTicks(GameConstants.getInTicks(0, 40));
        this.sync();
    }

    public void setGlowTicks(int ticks) {
        this.glowTicks = ticks;
        this.sync();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.BARTENDER);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.glowTicks > 0);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.glowTicks = buf.readBoolean() ? 1 : 0;
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("glowTicks", this.glowTicks);
        tag.putInt("pendingEffectDelay", this.pendingEffectDelay);
        tag.putInt("whiskeyShieldTicks", this.whiskeyShieldTicks);
        tag.putBoolean("vodkaStimulationActive", this.vodkaStimulationActive);
        if (!this.pendingIngredients.isEmpty()) {
            NbtList list = new NbtList();
            for (String id : this.pendingIngredients) {
                list.add(NbtString.of(id));
            }
            tag.put("pendingIngredients", list);
        }
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.glowTicks = tag.contains("glowTicks") ? tag.getInt("glowTicks") : 0;
        this.pendingEffectDelay = tag.contains("pendingEffectDelay") ? tag.getInt("pendingEffectDelay") : 0;
        this.whiskeyShieldTicks = tag.contains("whiskeyShieldTicks") ? tag.getInt("whiskeyShieldTicks") : 0;
        this.vodkaStimulationActive = tag.contains("vodkaStimulationActive") && tag.getBoolean("vodkaStimulationActive");
        this.pendingIngredients.clear();
        if (tag.contains("pendingIngredients")) {
            NbtList list = tag.getList("pendingIngredients", NbtString.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                this.pendingIngredients.add(list.getString(i));
            }
        }
    }
}
