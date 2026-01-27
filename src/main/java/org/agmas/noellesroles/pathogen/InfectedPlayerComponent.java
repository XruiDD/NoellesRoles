package org.agmas.noellesroles.pathogen;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

public class InfectedPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<InfectedPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "infected"), InfectedPlayerComponent.class);
    private final PlayerEntity player;
    private boolean infected = false;
    private UUID infectedBy = null;

    // 延迟音效播放字段
    private int sneezeSoundDelay = 0;

    public InfectedPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void reset() {
        this.infected = false;
        this.infectedBy = null;
        this.sneezeSoundDelay = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(recipient.getWorld());
        return gameWorldComponent.isRole(recipient, Noellesroles.PATHOGEN);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.infected);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.infected = buf.readBoolean();
    }

    @Override
    public void serverTick() {
        // 处理延迟音效播放
        if (sneezeSoundDelay > 0) {
            sneezeSoundDelay--;

            // 延迟结束，在被感染者当前位置播放打喷嚏音效并发送actionbar提醒
            if (sneezeSoundDelay == 0) {
                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.playSound(
                        null,
                        player.getBlockPos(),
                        SoundEvents.ENTITY_PANDA_SNEEZE,
                        SoundCategory.PLAYERS,
                        2.0F,
                        0.8F
                    );

                    // 发送actionbar消息提醒玩家生病了
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.sendMessage(Text.translatable("noellesroles.infected.notification"), true);
                    }
                }
            }
        }
    }

    /**
     * 设置延迟音效播放
     * @param delayTicks 延迟的tick数（10-30秒 = 200-600 ticks）
     */
    public void scheduleSneezeSound(int delayTicks) {
        this.sneezeSoundDelay = delayTicks;
    }

    public void setInfected(boolean infected, UUID pathogenUuid) {
        boolean wasInfected = this.infected;
        this.infected = infected;
        this.infectedBy = pathogenUuid;
        this.sync();

        // 如果是首次被感染，且还没有设置打喷嚏延迟，则设置一个随机延迟（10-30秒）
        if (infected && !wasInfected && this.sneezeSoundDelay == 0 && player.getWorld() instanceof ServerWorld serverWorld) {
            int delayTicks = 200 + serverWorld.random.nextInt(401); // 200 + [0, 400] = [200, 600] = 10-30秒
            this.scheduleSneezeSound(delayTicks);
        }

        // 如果被感染，检查是否在饕餮肚子里，如果是则传播给肚子里的所有人
        if (infected && pathogenUuid != null && player.getWorld() instanceof ServerWorld serverWorld) {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(serverWorld);

            // 情况1：如果被感染者是饕餮本身
            if (gameWorldComponent.isRole(player, Noellesroles.TAOTIE)) {
                TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(player);
                taotieComp.spreadInfectionInStomach(pathogenUuid);
            }
            // 情况2：如果被感染者在饕餮肚子里
            else {
                SwallowedPlayerComponent swallowedComp = SwallowedPlayerComponent.KEY.get(player);
                if (swallowedComp.isSwallowed()) {
                    UUID taotieUuid = swallowedComp.getSwallowedBy();
                    if (taotieUuid != null) {
                        PlayerEntity taotie = serverWorld.getPlayerByUuid(taotieUuid);
                        if (taotie != null && GameFunctions.isPlayerAliveAndSurvival(taotie)) {
                            TaotiePlayerComponent taotieComp = TaotiePlayerComponent.KEY.get(taotie);
                            taotieComp.spreadInfectionInStomach(pathogenUuid);
                        }
                    }
                }
            }
        }
    }

    public boolean isInfected() {
        return infected;
    }

    public UUID getInfectedBy() {
        return infectedBy;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("infected", this.infected);
        if (this.infectedBy != null) {
            tag.putUuid("infectedBy", this.infectedBy);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.infected = tag.getBoolean("infected");
        if (tag.contains("infectedBy")) {
            this.infectedBy = tag.getUuid("infectedBy");
        } else {
            this.infectedBy = null;
        }
    }
}
