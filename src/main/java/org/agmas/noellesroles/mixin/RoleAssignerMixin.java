package org.agmas.noellesroles.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.cca.PlayerNoteComponent;
import dev.doctor4t.trainmurdermystery.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.trainmurdermystery.game.GameFunctions;
import dev.doctor4t.trainmurdermystery.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.ModdedRole;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.RoleHelpers;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Mixin(GameFunctions.class)
public abstract class RoleAssignerMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void jesterWrite(ServerPlayerEntity player, CallbackInfo ci) {

        ((MorphlingPlayerComponent)MorphlingPlayerComponent.KEY.get(player)).reset();
    }
    @Inject(method = "assignRolesAndGetKillerCount", at = @At("TAIL"))
    private static void jesterWrite(@NotNull ServerWorld world, @NotNull List<ServerPlayerEntity> players, GameWorldComponent gameComponent, CallbackInfoReturnable<Integer> cir) {

        boolean allRolesFilled = false;
        int desiredRoleCount = (int)Math.floor((double)((float)players.size() * 0.2F));
        for (ModdedRole moddedRole : RoleHelpers.instance.getModdedRoles()) {
            moddedRole.resetPlayerList();
        }


        ArrayList<ModdedRole> shuffledRoles = new ArrayList<>(RoleHelpers.instance.getModdedRoles());
        Collections.shuffle(shuffledRoles);

        // There's an entire complex system for rat's roles but... I'm just gonna randomly set the roles with a shuffle. Lol

        ArrayList<ServerPlayerEntity> playersForCivillianRoles = new ArrayList<>();

        players.forEach((p) -> {
            if (!gameComponent.isVigilante(p) && !gameComponent.isKiller(p)) {
                playersForCivillianRoles.add(p);
            }
        });

        while (!allRolesFilled) {
            allRolesFilled = true;
            for (ModdedRole moddedRole : shuffledRoles) {
                if (moddedRole.isKiller) continue;
                if (moddedRole.maxCount > 0 && moddedRole.maxCount <= moddedRole.getPlayers().size()) continue;
                if (moddedRole.getPlayers().size() >= desiredRoleCount) continue;
                Collections.shuffle(playersForCivillianRoles);

                assignOneOfRole(moddedRole, playersForCivillianRoles);
                allRolesFilled = false;
            }
        }


        ArrayList<ServerPlayerEntity> playersForKillerRoles = new ArrayList<>();

        players.forEach((p) -> {
            if (gameComponent.isKiller(p)) {
                playersForKillerRoles.add(p);
            }
        });

        allRolesFilled = false;

        while (!allRolesFilled) {
            allRolesFilled = true;
            for (ModdedRole moddedRole : shuffledRoles) {
                if (!moddedRole.isKiller) continue;
                if (moddedRole.maxCount > 0 && moddedRole.maxCount <= moddedRole.getPlayers().size()) continue;
                if (moddedRole.getPlayers().size() >= desiredRoleCount) continue;

                Collections.shuffle(playersForKillerRoles);

                assignOneOfRole(moddedRole,playersForKillerRoles);
                allRolesFilled = false;
            }
        }


    }

    @Redirect(method = "initializeGame", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking;send(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/packet/CustomPayload;)V", ordinal = 1))
    private static void jesterWrite(ServerPlayerEntity player, CustomPayload payload, @Local int killerCount, @Local List<ServerPlayerEntity> players) {

        if (RoleHelpers.instance.isOfAnyModdedRole(player)) {
            ModdedRole role = RoleHelpers.instance.getRoleOfPlayer(player);
            Log.info(LogCategory.GENERAL, "sent " + role.packet_id + " to " + player.getName());
            ServerPlayNetworking.send(player, new AnnounceWelcomePayload(role.packet_id, killerCount, players.size()-killerCount));
        } else {
            ServerPlayNetworking.send(player,payload);
        }
    }


    @Unique
    private static void assignOneOfRole(ModdedRole role, @NotNull List<ServerPlayerEntity> players) {
        for (PlayerEntity p : players) {
            if (!RoleHelpers.instance.isOfAnyModdedRole(p)) {
                role.addPlayer(p);
                role.onGameStarted(p);
                Log.info(LogCategory.GENERAL, "Gave " + role.translationKey + " to " + p.getName());
                return;

            }
        }
    }
}
