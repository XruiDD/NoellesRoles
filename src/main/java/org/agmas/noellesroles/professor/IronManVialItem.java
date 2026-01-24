package org.agmas.noellesroles.professor;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;

public class IronManVialItem extends Item {
    // 5分钟冷却 = 5 * 60 * 20 ticks
    private static final int COOLDOWN_TICKS = 5 * 60 * 20;

    public IronManVialItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();
        if (world.isClient) {
            return ActionResult.PASS;
        }

        if (user.getItemCooldownManager().isCoolingDown(this)){
            return ActionResult.FAIL;
        }

        // 目标必须是玩家
        if (!(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }

        // 目标必须存活
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
            return ActionResult.PASS;
        }

        // Check if target already has iron man buff
        IronManPlayerComponent targetComp = IronManPlayerComponent.KEY.get(target);
        if (targetComp.hasBuff()) {
            return ActionResult.PASS;
        }

        // Apply iron man buff to target
        targetComp.applyBuff();

        // Play sound
        world.playSound(null, target.getBlockPos(), WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.PLAYERS, 1.0F, 1.0F);

        // Set cooldown (5 minutes)
        user.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        return ActionResult.SUCCESS;
    }
}
