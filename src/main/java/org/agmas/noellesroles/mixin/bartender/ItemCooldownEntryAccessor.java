package org.agmas.noellesroles.mixin.bartender;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface ItemCooldownEntryAccessor {
    @Accessor("startTick")
    int getStartTick();

    @Accessor("endTick")
    int getEndTick();
}
