package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

import java.util.List;

/**
 * 催化剂物品
 * - 对中毒玩家使用：5秒内中毒死亡，重置毒针冷却
 * - 对空气/非中毒玩家使用：仅重置毒针冷却
 * - 一次性消耗品
 */
public class CatalystItem extends Item {
    private static final int CATALYZED_POISON_TICKS = 20 * 5; // 5秒

    public CatalystItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.catalyst.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();

        if (!(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
                return ActionResult.PASS;
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            if (!gameWorld.isRole(user, Noellesroles.POISONER)) {
                return ActionResult.PASS;
            }

            PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(target);
            if (poisonComp.poisonTicks > 0) {
                // 中毒玩家：催化为5秒内死亡
                poisonComp.poisoner = user.getUuid();
                poisonComp.poisonTicks = CATALYZED_POISON_TICKS;
                poisonComp.sync();

                // 记录使用
                if (user instanceof ServerPlayerEntity serverUser) {
                    GameRecordManager.recordItemUse(serverUser, Registries.ITEM.getId(this), (ServerPlayerEntity) target, null);
                }
            }

            // 无论目标是否中毒，都重置毒针冷却并消耗物品
            user.getItemCooldownManager().remove(ModItems.POISON_NEEDLE);
            stack.decrementUnlessCreative(1, user);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if (!world.isClient) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            if (!gameWorld.isRole(user, Noellesroles.POISONER)) {
                return TypedActionResult.pass(itemStack);
            }

            // 对空气使用：仅重置毒针冷却并消耗
            user.getItemCooldownManager().remove(ModItems.POISON_NEEDLE);
            itemStack.decrementUnlessCreative(1, user);
            return TypedActionResult.success(itemStack);
        }

        return TypedActionResult.success(itemStack, true);
    }
}
