package org.agmas.noellesroles.mixin.wathe;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.deatharena.DeathArenaStateHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRecordManager.class)
public class GameRecordManagerArenaFilterMixin {
    @Inject(
            method = "addEvent(Lnet/minecraft/server/world/ServerWorld;Ljava/lang/String;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/nbt/NbtCompound;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void noellesroles$skipArenaEventAtFinalRecordPoint(
            ServerWorld world,
            String type,
            ServerPlayerEntity actor,
            ServerPlayerEntity target,
            NbtCompound data,
            CallbackInfo ci) {
        if (DeathArenaStateHelper.isDeathArenaDimension(world)
                || DeathArenaStateHelper.isDeathArenaParticipant(actor)
                || DeathArenaStateHelper.isDeathArenaParticipant(target)) {
            ci.cancel();
        }
    }

    @Inject(method = "recordDeath", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$skipArenaDeathRecord(
            ServerPlayerEntity victim,
            ServerPlayerEntity killer,
            Identifier deathReason,
            CallbackInfo ci) {
        if (DeathArenaStateHelper.isDeathArenaParticipant(victim)
                || DeathArenaStateHelper.isDeathArenaParticipant(killer)) {
            ci.cancel();
        }
    }

    @Inject(method = "recordItemUse", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$skipArenaItemUseRecord(
            ServerPlayerEntity actor,
            Identifier itemId,
            ServerPlayerEntity target,
            NbtCompound extra,
            CallbackInfo ci) {
        if (DeathArenaStateHelper.isDeathArenaParticipant(actor)
                || DeathArenaStateHelper.isDeathArenaParticipant(target)) {
            ci.cancel();
        }
    }

    @Inject(method = "recordSkillUse", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$skipArenaSkillUseRecord(
            ServerPlayerEntity actor,
            Identifier skillId,
            ServerPlayerEntity target,
            NbtCompound extra,
            CallbackInfo ci) {
        if (DeathArenaStateHelper.isDeathArenaParticipant(actor)
                || DeathArenaStateHelper.isDeathArenaParticipant(target)) {
            ci.cancel();
        }
    }

    @Inject(method = "recordGlobalEvent", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$skipArenaGlobalEventRecord(
            ServerWorld world,
            Identifier eventId,
            ServerPlayerEntity actor,
            NbtCompound extra,
            CallbackInfo ci) {
        if (DeathArenaStateHelper.isDeathArenaParticipant(actor)) {
            ci.cancel();
        }
    }
}
