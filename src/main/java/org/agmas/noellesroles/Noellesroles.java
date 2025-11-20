package org.agmas.noellesroles;

import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.packet.MorphC2SPacket;

import java.awt.*;

public class Noellesroles implements ModInitializer {

    public static String MOD_ID = "noellesroles";

    public static Identifier JESTER_ID = Identifier.of(MOD_ID, "jester");
    public static Identifier MORPHLING_ID = Identifier.of(MOD_ID, "morphling");
    public static Identifier HOST_ID = Identifier.of(MOD_ID, "host");
    public static Identifier AWESOME_BINGLUS_ID = Identifier.of(MOD_ID, "awesome_binglus");

    public static final CustomPayload.Id<MorphC2SPacket> MORPH_PACKET = MorphC2SPacket.ID;
    @Override
    public void onInitialize() {
        ModItems.init();

        RoleHelpers.registerNewTypedRole(new JesterRole(JESTER_ID, new Color(255,86,243).getRGB(),true,false,0,0));
        RoleHelpers.registerNewRole(MORPHLING_ID, new Color(170, 2, 61).getRGB(),true,true,2);
        RoleHelpers.registerNewTypedRole(new HostRole(HOST_ID, new Color(255, 205, 84).getRGB(),false,false,1,0));
        RoleHelpers.registerNewTypedRole(new AwesomeBinglusRole(AWESOME_BINGLUS_ID, new Color(155, 255, 168).getRGB(),false,false,0,0));
        PayloadTypeRegistry.playC2S().register(MorphC2SPacket.ID, MorphC2SPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.MORPH_PACKET, (payload, context) -> {
            MorphlingPlayerComponent morphlingPlayerComponent = (MorphlingPlayerComponent) MorphlingPlayerComponent.KEY.get(context.player());
            morphlingPlayerComponent.startMorph(payload.player());
        });
    }
}
