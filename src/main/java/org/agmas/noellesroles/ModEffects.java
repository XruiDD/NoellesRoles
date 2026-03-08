package org.agmas.noellesroles;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.effect.GinImmunityEffect;
import org.agmas.noellesroles.effect.StimulationEffect;
import org.agmas.noellesroles.effect.NoCollisionEffect;
import org.agmas.noellesroles.effect.WhiskeyShieldEffect;

public class ModEffects {
    public static final RegistryEntry<StatusEffect> STIMULATION = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(Noellesroles.MOD_ID, "stimulation"),
            new StimulationEffect()
    );

    public static final RegistryEntry<StatusEffect> NO_COLLISION = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(Noellesroles.MOD_ID, "no_collision"),
            new NoCollisionEffect()
    );

    public static final RegistryEntry<StatusEffect> GIN_IMMUNITY = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(Noellesroles.MOD_ID, "gin_immunity"),
            new GinImmunityEffect()
    );

    public static final RegistryEntry<StatusEffect> WHISKEY_SHIELD = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(Noellesroles.MOD_ID, "whiskey_shield"),
            new WhiskeyShieldEffect()
    );

    public static void init() {
    }
}
