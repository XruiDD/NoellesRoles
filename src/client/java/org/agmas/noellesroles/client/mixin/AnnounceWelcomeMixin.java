package org.agmas.noellesroles.client.mixin;

import dev.doctor4t.trainmurdermystery.client.gui.RoleAnnouncementText;
import dev.doctor4t.trainmurdermystery.client.gui.RoundTextRenderer;
import dev.doctor4t.trainmurdermystery.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.RoleHelpers;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AnnounceWelcomePayload.Receiver.class)
public abstract class AnnounceWelcomeMixin {

    @Inject(method = "receive(Ldev/doctor4t/trainmurdermystery/util/AnnounceWelcomePayload;Lnet/fabricmc/fabric/api/client/networking/v1/ClientPlayNetworking$Context;)V", at = @At("HEAD"), cancellable = true)
    void b(AnnounceWelcomePayload payload, ClientPlayNetworking.Context context, CallbackInfo ci) {
        Log.info(LogCategory.GENERAL, payload.role()+"");
        if (payload.role() > 100) {
            NoellesrolesClient.clientModdedRole = RoleHelpers.instance.getRoleFromAnnouncerID(payload.role());
            RoundTextRenderer.startWelcome(NoellesrolesClient.clientModdedRole.winsWithKillers || NoellesrolesClient.clientModdedRole.isKiller ? RoleAnnouncementText.KILLER : RoleAnnouncementText.CIVILIAN, payload.killers(), payload.targets());
            ci.cancel();
        } else {
            NoellesrolesClient.clientModdedRole = null;
        }
    }
}
