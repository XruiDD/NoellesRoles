package org.agmas.noellesroles.mixin.waiter;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.CocktailItem;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 服务员喂食机制：手持食物/饮品右键点击其他玩家，即时喂食，
 * 帮助目标完成食物/饮品需求任务，消耗手中物品。
 */
@Mixin(Item.class)
public class WaiterFeedingMixin {

    @Inject(method = "useOnEntity", at = @At("HEAD"), cancellable = true)
    private void waiterFeedPlayer(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand,
                                   CallbackInfoReturnable<ActionResult> cir) {
        if (user.getWorld().isClient()) return;
        if (!(entity instanceof ServerPlayerEntity target)) return;
        if (!(user instanceof ServerPlayerEntity serverUser)) return;

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(user.getWorld());
        if (!gameWorld.isRole(user, Noellesroles.WAITER)) return;
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) return;

        // 判断是饮品还是食物
        boolean isDrink = stack.getItem() instanceof CocktailItem;
        boolean isFood = !isDrink && stack.get(DataComponentTypes.FOOD) != null;
        if (!isDrink && !isFood) return;

        // 检查目标是否有对应的需求任务
        PlayerMoodComponent targetMood = PlayerMoodComponent.KEY.get(target);
        PlayerMoodComponent.Task taskType = isDrink ? PlayerMoodComponent.Task.DRINK : PlayerMoodComponent.Task.EAT;
        PlayerMoodComponent.TrainTask task = targetMood.tasks.get(taskType);
        if (task == null || task.isFulfilled(target)) {
            serverUser.sendMessage(Text.translatable(
                    isDrink ? "tip.waiter.target_not_thirsty" : "tip.waiter.target_not_hungry",
                    target.getName()), true);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // 即时喂食
        Text itemName = stack.getName();
        if (isDrink) {
            targetMood.drinkCocktail();
            user.getWorld().playSound(null, target.getBlockPos(),
                    SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
        } else {
            targetMood.eatFood();
            user.getWorld().playSound(null, target.getBlockPos(),
                    SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }

        // 消耗物品
        if (!user.isCreative()) {
            stack.decrement(1);
        }

        // 记录技能使用（附带物品信息）
        NbtCompound extra = new NbtCompound();
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        extra.putString("item", itemId.toString());
        extra.putString("item_name", Text.Serialization.toJsonString(
                itemName, serverUser.getRegistryManager()));
        extra.putString("type", isDrink ? "drink" : "food");
        GameRecordManager.recordSkillUse(serverUser, Noellesroles.WAITER_ID, target, extra);

        // 通知双方
        serverUser.sendMessage(Text.translatable("tip.waiter.feed_complete", target.getName(), itemName), true);
        target.sendMessage(Text.translatable("tip.waiter.fed_by", serverUser.getName(), itemName), true);

        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
