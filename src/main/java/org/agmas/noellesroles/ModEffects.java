package org.agmas.noellesroles;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.effect.EuphoriaEffect;
import org.agmas.noellesroles.effect.NoCollisionEffect;

public class ModEffects {
    public static final RegistryEntry<StatusEffect> EUPHORIA = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(Noellesroles.MOD_ID, "euphoria"),
            new EuphoriaEffect()
    );

    public static final RegistryEntry<StatusEffect> NO_COLLISION = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(Noellesroles.MOD_ID, "no_collision"),
            new NoCollisionEffect()
    );

    public static void init() {
    }
}
