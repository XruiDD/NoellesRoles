package org.agmas.noellesroles.orthopedist;

import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheAttributes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class OrthopedistPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<OrthopedistPlayerComponent> KEY = ComponentRegistry.getOrCreate(
        Identifier.of(Noellesroles.MOD_ID, "orthopedist"), OrthopedistPlayerComponent.class
    );

    private static final Identifier PASSIVE_STAMINA_MODIFIER_ID = Identifier.of(Noellesroles.MOD_ID, "orthopedist_passive_stamina");
    private static final Identifier BONE_SETTING_STAMINA_MODIFIER_ID = Identifier.of(Noellesroles.MOD_ID, "bone_setting_stamina");
    private static final double PASSIVE_STAMINA_BONUS = 0.25D;
    private static final double BONE_SETTING_STAMINA_BONUS = 1.0D / 3.0D;

    private final PlayerEntity player;
    private boolean passiveApplied = false;

    public OrthopedistPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.passiveApplied = false;
        removeModifier(PASSIVE_STAMINA_MODIFIER_ID);
        removeModifier(BONE_SETTING_STAMINA_MODIFIER_ID);
        this.player.removeStatusEffect(ModEffects.BONE_SETTING);
        this.sync();
    }

    public static void applyBoneSetting(ServerPlayerEntity target) {
        target.addStatusEffect(new StatusEffectInstance(ModEffects.BONE_SETTING, GameConstants.getInTicks(0, 20), 0, false, true, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, GameConstants.getInTicks(0, 5), 0, false, true, true));
        refreshBoneSetting(target);
    }

    public static void refreshBoneSetting(ServerPlayerEntity player) {
        boolean active = player.hasStatusEffect(ModEffects.BONE_SETTING);
        syncModifier(player, BONE_SETTING_STAMINA_MODIFIER_ID, BONE_SETTING_STAMINA_BONUS, active);
    }

    @Override
    public void serverTick() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverPlayer.getWorld());
        boolean isOrthopedist = gameWorld.isRole(serverPlayer, Noellesroles.ORTHOPEDIST);

        if (isOrthopedist && !this.passiveApplied) {
            this.passiveApplied = true;
            applyModifier(PASSIVE_STAMINA_MODIFIER_ID, PASSIVE_STAMINA_BONUS);
            this.sync();
        } else if (!isOrthopedist && this.passiveApplied) {
            this.passiveApplied = false;
            removeModifier(PASSIVE_STAMINA_MODIFIER_ID);
            this.sync();
        }

        boolean hasBoneSetting = serverPlayer.hasStatusEffect(ModEffects.BONE_SETTING);
        if (hasBoneSetting) {
            applyModifier(BONE_SETTING_STAMINA_MODIFIER_ID, BONE_SETTING_STAMINA_BONUS);
        } else {
            removeModifier(BONE_SETTING_STAMINA_MODIFIER_ID);
        }
    }

    private void applyModifier(Identifier modifierId, double amount) {
        EntityAttributeInstance attribute = this.player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (attribute == null || attribute.hasModifier(modifierId)) {
            return;
        }
        withStaminaRatio(attribute, () ->
            attribute.addTemporaryModifier(new EntityAttributeModifier(
                modifierId,
                amount,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ))
        );
    }

    private void removeModifier(Identifier modifierId) {
        EntityAttributeInstance attribute = this.player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        if (attribute == null || !attribute.hasModifier(modifierId)) {
            return;
        }
        withStaminaRatio(attribute, () -> attribute.removeModifier(modifierId));
    }

    private void withStaminaRatio(EntityAttributeInstance attribute, Runnable modifierAction) {
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(this.player);
        double oldMax = attribute.getValue();
        double ratio = oldMax > 0.0D ? Math.max(0.0D, Math.min(1.0D, stamina.getSprintingTicks() / oldMax)) : 1.0D;
        modifierAction.run();
        int newMax = (int) Math.round(attribute.getValue());
        stamina.setMaxSprintTime(newMax);
        stamina.setSprintingTicks((int) Math.round(newMax * ratio));
        stamina.sync();
    }

    private static void syncModifier(ServerPlayerEntity player, Identifier modifierId, double amount, boolean active) {
        OrthopedistPlayerComponent component = KEY.get(player);
        if (active) {
            component.applyModifier(modifierId, amount);
        } else {
            component.removeModifier(modifierId);
        }
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
        buf.writeBoolean(this.passiveApplied);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.passiveApplied = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("passiveApplied", this.passiveApplied);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.passiveApplied = tag.getBoolean("passiveApplied");
    }
}
