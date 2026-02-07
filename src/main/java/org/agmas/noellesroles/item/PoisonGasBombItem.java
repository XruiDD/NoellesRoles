package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.entity.PoisonGasBombEntity;

import java.util.List;

/**
 * 毒气弹物品
 * - 右键丢出毒气弹
 * - 消耗物品
 */
public class PoisonGasBombItem extends Item {
    public PoisonGasBombItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.poison_gas_bomb.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient) {
            PoisonGasBombEntity gasBomb = new PoisonGasBombEntity(NoellesRolesEntities.POISON_GAS_BOMB_ENTITY, world);
            gasBomb.setOwner(user);
            gasBomb.setPos(user.getX(), user.getEyeY() - 0.1, user.getZ());
            gasBomb.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.6875F, 1.0F);
            world.spawnEntity(gasBomb);
            if (user instanceof ServerPlayerEntity serverPlayer) {
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), null, null);
            }
        }

        user.incrementStat(Stats.USED.getOrCreateStat(this));
        itemStack.decrementUnlessCreative(1, user);
        return TypedActionResult.success(itemStack, world.isClient());
    }
}
