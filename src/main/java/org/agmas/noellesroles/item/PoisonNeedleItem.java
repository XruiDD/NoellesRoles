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
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;

import java.util.List;

/**
 * 毒针物品
 * - 右键点击玩家注入毒素
 * - 可重复使用，使用后45秒冷却
 * - 开局1分钟冷却
 */
public class PoisonNeedleItem extends Item {
    public static final int USE_COOLDOWN_TICKS = 20 * 45; // 45秒冷却

    public PoisonNeedleItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.poison_needle.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();

        if (!(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }

        if (user.getItemCooldownManager().isCoolingDown(this)) {
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

            // 随机中毒时间 800-1400 ticks
            int poisonTicks = PlayerPoisonComponent.clampTime.getLeft() +
                    world.random.nextInt(PlayerPoisonComponent.clampTime.getRight() - PlayerPoisonComponent.clampTime.getLeft() + 1);

            PlayerPoisonComponent.KEY.get(target).setPoisonTicks(poisonTicks, user.getUuid(), Noellesroles.POISON_SOURCE_NEEDLE);

            // 设置冷却
            user.getItemCooldownManager().set(this, USE_COOLDOWN_TICKS);

            // 记录使用
            if (user instanceof ServerPlayerEntity serverUser) {
                GameRecordManager.recordItemUse(serverUser, Registries.ITEM.getId(this), (ServerPlayerEntity) target, null);
            }
        }

        return ActionResult.SUCCESS;
    }
}
