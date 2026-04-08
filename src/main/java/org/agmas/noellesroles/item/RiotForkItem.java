package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
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
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.riotpatrol.RiotPatrolPlayerComponent;

import java.util.List;

public class RiotForkItem extends Item {
    public static final int ROOT_DURATION_TICKS = 20 * 8;
    public static final int COOLDOWN_TICKS = 20 * 60;
    public static final double MAX_RANGE = 3.0;

    public RiotForkItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return ActionResult.PASS;
        }
        if (!GameWorldComponent.KEY.get(user.getWorld()).isRole(user, Noellesroles.RIOT_PATROL)) {
            return ActionResult.PASS;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(user) || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return ActionResult.PASS;
        }
        if (user.squaredDistanceTo(target) > MAX_RANGE * MAX_RANGE) {
            return ActionResult.PASS;
        }

        if (!user.getWorld().isClient) {
            RiotPatrolPlayerComponent.KEY.get(user).startForkRoot(target, ROOT_DURATION_TICKS, true);
            RiotPatrolPlayerComponent.KEY.get(target).startForkRoot(user, ROOT_DURATION_TICKS, false);
            user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            user.getWorld().playSound(null, user.getBlockPos(), SoundEvents.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0F, 0.9F);

            if (target instanceof ServerPlayerEntity serverTarget) {
                serverTarget.sendMessage(Text.translatable("tip.noellesroles.riot_fork.rooted"), true);
            }
            if (user instanceof ServerPlayerEntity serverUser && target instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordItemUse(serverUser, Registries.ITEM.getId(this), serverTarget, null);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.riot_fork.tooltip"));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
