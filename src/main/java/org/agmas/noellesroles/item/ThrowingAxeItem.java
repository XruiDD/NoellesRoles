package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.entity.ThrowingAxeEntity;

import java.util.List;

public class ThrowingAxeItem extends Item {

    public ThrowingAxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) return;

        int useDuration = this.getMaxUseTime(stack, user) - remainingUseTicks;
        float power = getPowerForTime(useDuration);

        if (power < 0.25F) return;

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);

        if (!world.isClient) {
            ThrowingAxeEntity axeEntity = new ThrowingAxeEntity(NoellesRolesEntities.THROWING_AXE_ENTITY, world);
            axeEntity.setOwner(player);
            axeEntity.setPosition(player.getX(), player.getEyeY() - 0.1, player.getZ());
            float velocity = 0.4F + (power * 2.0F);
            axeEntity.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, velocity, 1.0F);
            world.spawnEntity(axeEntity);

            if (player instanceof ServerPlayerEntity serverPlayer) {
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), null, null);
            }
        }

        stack.decrementUnlessCreative(1, player);
        player.incrementStat(Stats.USED.getOrCreateStat(this));
    }

    public static float getPowerForTime(int time) {
        float f = (float) time / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        return f;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.throwing_axe.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
