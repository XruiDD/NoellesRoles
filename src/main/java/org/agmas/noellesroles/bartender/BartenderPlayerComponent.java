package org.agmas.noellesroles.bartender;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class BartenderPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<BartenderPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "bartender"), BartenderPlayerComponent.class);
    public static final int INITIAL_PRICE = 100;
    public static final int MAX_PRICE = 300;
    public static final int PRICE_INCREMENT = 50;

    private final PlayerEntity player;
    public int glowTicks = 0;
    public boolean armor = false;
    public int currentPrice = INITIAL_PRICE;

    public void reset() {
        this.glowTicks = 0;
        this.armor = false;
        this.currentPrice = INITIAL_PRICE;
        this.sync();
    }

    public BartenderPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void serverTick() {
        if (this.glowTicks > 0) {
            --this.glowTicks;
        }
    }

    public void giveArmor() {
        armor = true;
        this.sync();
    }


    public void startGlow() {
        setGlowTicks(GameConstants.getInTicks(0,40));
        this.sync();
    }


    public void setGlowTicks(int ticks) {
        this.glowTicks = ticks;
        this.sync();
    }

    public int getCurrentPrice() {
        return this.currentPrice;
    }

    public void increasePrice() {
        if (this.currentPrice < MAX_PRICE) {
            this.currentPrice += PRICE_INCREMENT;
            if (this.currentPrice > MAX_PRICE) {
                this.currentPrice = MAX_PRICE;
            }
            this.sync();
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.BARTENDER);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient){
        buf.writeBoolean(this.glowTicks > 0);
        buf.writeBoolean(this.armor);
        buf.writeInt(this.currentPrice);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.glowTicks = buf.readBoolean() ? 1 : 0;
        this.armor = buf.readBoolean();
        this.currentPrice = buf.readInt();
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("glowTicks", this.glowTicks);
        tag.putBoolean("armor", this.armor);
        tag.putInt("currentPrice", this.currentPrice);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.glowTicks = tag.contains("glowTicks") ? tag.getInt("glowTicks") : 0;
        if (tag.contains("armor")) {
            this.armor = tag.getBoolean("armor");
        } else {
            this.armor = false;
        }
        this.currentPrice = tag.contains("currentPrice") ? tag.getInt("currentPrice") : INITIAL_PRICE;
    }
}
