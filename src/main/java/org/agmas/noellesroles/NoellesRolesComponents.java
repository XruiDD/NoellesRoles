package org.agmas.noellesroles;


import dev.doctor4t.trainmurdermystery.cca.*;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;

public class NoellesRolesComponents implements EntityComponentInitializer {
    public NoellesRolesComponents() {
    }

    public void registerEntityComponentFactories(@NotNull EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(PlayerEntity.class, MorphlingPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(MorphlingPlayerComponent::new);
    }

}