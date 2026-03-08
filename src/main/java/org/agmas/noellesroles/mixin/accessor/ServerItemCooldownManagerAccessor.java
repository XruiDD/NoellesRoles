package org.agmas.noellesroles.mixin.accessor;

import net.minecraft.server.network.ServerItemCooldownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerItemCooldownManager.class)
public interface ServerItemCooldownManagerAccessor {
    @Accessor("player")
    ServerPlayerEntity getPlayer();
}
