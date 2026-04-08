package org.agmas.noellesroles.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.riotpatrol.RiotPatrolPlayerComponent;

import java.util.List;

public class RiotShieldItem extends Item {
    public static final int SHIELD_COOLDOWN_TICKS = 20 * 30;

    public RiotShieldItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        user.setCurrentHand(hand);
        if (!world.isClient) {
            RiotPatrolPlayerComponent.KEY.get(user).raiseShield();
        }
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && user instanceof PlayerEntity player) {
            RiotPatrolPlayerComponent.KEY.get(player).lowerShield(true);
        }
        super.onStoppedUsing(stack, world, user, remainingUseTicks);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.riot_shield.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
