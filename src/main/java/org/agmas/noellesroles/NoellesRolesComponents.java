package org.agmas.noellesroles;


import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.agmas.noellesroles.pathogen.InfectedPlayerComponent;
import org.agmas.noellesroles.pathogen.PathogenPlayerComponent;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.agmas.noellesroles.recaller.RecallerPlayerComponent;
import org.agmas.noellesroles.voodoo.VoodooPlayerComponent;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.vulture.VulturePlayerComponent;
import org.agmas.noellesroles.corruptcop.CorruptCopPlayerComponent;
import org.agmas.noellesroles.reporter.ReporterPlayerComponent;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.silencer.SilencedPlayerComponent;
import org.agmas.noellesroles.silencer.SilencerPlayerComponent;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.agmas.noellesroles.survivalmaster.SurvivalMasterPlayerComponent;
import org.agmas.noellesroles.music.WorldMusicComponent;
import org.agmas.noellesroles.scavenger.HiddenBodiesWorldComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;

public class NoellesRolesComponents implements EntityComponentInitializer, WorldComponentInitializer {
    public NoellesRolesComponents() {
    }

    public void registerEntityComponentFactories(@NotNull EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(PlayerEntity.class, MorphlingPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(MorphlingPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BartenderPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BartenderPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, VoodooPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(VoodooPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, AbilityPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AbilityPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, RecallerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(RecallerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, VulturePlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(VulturePlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, JesterPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(JesterPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, InfectedPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(InfectedPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, PathogenPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(PathogenPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BomberPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BomberPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, AssassinPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AssassinPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, CorruptCopPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(CorruptCopPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, ReporterPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ReporterPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SerialKillerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SerialKillerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, IronManPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(IronManPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, TaotiePlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(TaotiePlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SwallowedPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SwallowedPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SilencedPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SilencedPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SilencerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SilencerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BodyguardPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BodyguardPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SurvivalMasterPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SurvivalMasterPlayerComponent::new);
    }

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry worldComponentFactoryRegistry) {
        worldComponentFactoryRegistry.register(ConfigWorldComponent.KEY, ConfigWorldComponent::new);
        worldComponentFactoryRegistry.register(WorldMusicComponent.KEY, WorldMusicComponent::new);
        worldComponentFactoryRegistry.register(HiddenBodiesWorldComponent.KEY, HiddenBodiesWorldComponent::new);
    }
}