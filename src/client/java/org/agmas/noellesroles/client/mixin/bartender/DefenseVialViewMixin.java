package org.agmas.noellesroles.client.mixin.bartender;

import dev.doctor4t.trainmurdermystery.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.trainmurdermystery.cca.GameWorldComponent;
import dev.doctor4t.trainmurdermystery.event.CanSeePoison;
import dev.doctor4t.trainmurdermystery.index.TMMParticles;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(BeveragePlateBlockEntity.class)
public class DefenseVialViewMixin {
    @Inject(method = "clientTick", at = @At("HEAD"), order = 1001, cancellable = true)
    private static void view(World world, BlockPos pos, BlockState state, BlockEntity blockEntity, CallbackInfo ci) {
        if (blockEntity instanceof BeveragePlateBlockEntity tray) {
            if (tray.getPoisoner() != null) {
                if (((GameWorldComponent) GameWorldComponent.KEY.get(world)).isRole(UUID.fromString(tray.getPoisoner()), Noellesroles.BARTENDER) && CanSeePoison.EVENT.invoker().visible(MinecraftClient.getInstance().player)) {
                    world.addParticle(ParticleTypes.HAPPY_VILLAGER, (double) ((float) pos.getX() + 0.5F), (double) pos.getY(), (double) ((float) pos.getZ() + 0.5F), (double) 0.0F, (double) 0.15F, (double) 0.0F);
                    ci.cancel();
                }
            }
        }
    }
}
