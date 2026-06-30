package org.agmas.noellesroles.mixin.shadowjester;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.shadowjester.ShadowJesterPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 影子小丑的「决斗刀」只对其对决搭档生效；背叛变身后保留的废刀对任何人都无效。
 * <p>在 {@link KnifeStabPayload.Receiver#receive} 的最前面拦截：无效目标直接取消整个处理，
 * 因此<strong>不会触发任何副作用</strong>（刺杀音效、刀冷却、KillPlayer 前置事件、回放记录等）——
 * 比用 KillPlayer.BEFORE 取消死亡更干净。
 * <p>输出位/终局对决的「真刀」({@link ShadowJesterPlayerComponent#isRealKnife()}) 不拦截，可杀任何人。
 */
@Mixin(KnifeStabPayload.Receiver.class)
public abstract class ShadowJesterKnifeMixin {

    @Inject(method = "receive", at = @At("HEAD"), cancellable = true)
    private void noellesroles$shadowJesterDuelKnife(KnifeStabPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        ShadowJesterPlayerComponent shadow = ShadowJesterPlayerComponent.KEY.get(player);

        if (game.isRole(player, Noellesroles.SHADOW_JESTER)) {
            if (shadow.isRealKnife()) return; // 真刀：可杀任何人，放行
            // 决斗刀：只对搭档生效，其余目标整刀失效（连音效/冷却都不触发）
            UUID partner = shadow.getPartnerUuid();
            Entity targetEntity = player.getServerWorld().getEntityById(payload.target());
            boolean targetIsPartner = targetEntity instanceof PlayerEntity tp
                    && partner != null && partner.equals(tp.getUuid());
            if (!targetIsPartner) {
                ci.cancel();
            }
        } else if (shadow.isBetrayalTrophy()) {
            // 背叛变身后保留的废刀：永久无效
            ci.cancel();
        }
    }
}
