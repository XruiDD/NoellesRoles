package org.agmas.noellesroles.mixin;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import org.agmas.noellesroles.RoleHelpers;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(GameWorldComponent.class)
public abstract class ModdedRoleNetworkMixin {

    @Shadow protected abstract ArrayList<UUID> uuidListFromNbt(NbtCompound nbtCompound, String listName);

    @Shadow protected abstract NbtList nbtFromUuidList(List<UUID> list);

    @Shadow public abstract boolean isCivilian(@NotNull PlayerEntity player);

    @Inject(method = "<init>", at = @At("TAIL"))
    public void jesterUpdate(World world, CallbackInfo ci) {
        RoleHelpers.instance = new RoleHelpers();
    }
    @Inject(method = "readFromNbt", at = @At("TAIL"))
    public void jesterRead(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup, CallbackInfo ci) {
        RoleHelpers.instance.getModdedRoles().forEach((m)->{
            if (nbtCompound.contains(m.networkKey)) {
                m.setPlayers(uuidListFromNbt(nbtCompound, m.networkKey));
            }
        });
    }
    @Inject(method = "writeToNbt", at = @At("TAIL"))
    public void jesterWrite(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup, CallbackInfo ci) {
        RoleHelpers.instance.getModdedRoles().forEach((m)->{
            nbtCompound.put(m.networkKey, nbtFromUuidList(m.getPlayers()));
        });
    }
    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Ldev/doctor4t/trainmurdermystery/cca/GameWorldComponent;isCivilian(Lnet/minecraft/entity/player/PlayerEntity;)Z"))
    public boolean winCondition(GameWorldComponent instance, PlayerEntity player) {
        if (RoleHelpers.instance.isOfAnyModdedRole(player)) {
            if (RoleHelpers.instance.getRoleOfPlayer(player).winsWithKillers) {
                return false;
            }
        }
        return isCivilian(player) && !GameFunctions.isPlayerEliminated(player);
    }



}
