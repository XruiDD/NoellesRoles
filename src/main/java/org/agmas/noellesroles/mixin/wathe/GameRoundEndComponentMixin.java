package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRoundEndComponent.class)
public class GameRoundEndComponentMixin {
    @Redirect(
            method = "didWin(Ljava/util/UUID;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/api/WatheRoles;getRole(Lnet/minecraft/util/Identifier;)Ldev/doctor4t/wathe/api/Role;"
            )
    )
    private Role noellesroles$guardMissingRoundEndRole(Identifier identifier) {
        Role role = WatheRoles.getRole(identifier);
        return role != null ? role : WatheRoles.NO_ROLE;
    }
}
