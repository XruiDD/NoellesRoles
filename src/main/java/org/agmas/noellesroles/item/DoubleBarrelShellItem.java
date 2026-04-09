package org.agmas.noellesroles.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

public class DoubleBarrelShellItem extends Item {
    public DoubleBarrelShellItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.double_barrel_shell.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack shells = user.getStackInHand(hand);
        ItemStack shotgun = user.getOffHandStack().isOf(org.agmas.noellesroles.ModItems.DOUBLE_BARREL_SHOTGUN)
            ? user.getOffHandStack()
            : user.getMainHandStack().isOf(org.agmas.noellesroles.ModItems.DOUBLE_BARREL_SHOTGUN)
                ? user.getMainHandStack()
                : ItemStack.EMPTY;

        if (shotgun.isEmpty()) {
            return TypedActionResult.pass(shells);
        }

        int loaded = DoubleBarrelShotgunItem.getLoadedShells(shotgun);
        if (loaded >= DoubleBarrelShotgunItem.MAX_SHELLS) {
            return TypedActionResult.fail(shells);
        }

        if (!world.isClient) {
            DoubleBarrelShotgunItem.setLoadedShells(shotgun, loaded + 1);
            shells.decrement(1);
        }
        return TypedActionResult.success(shells, world.isClient);
    }
}
