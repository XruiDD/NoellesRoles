package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.item.CocktailItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

public class FineDrinkItem extends CocktailItem {
    public FineDrinkItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof PlayerEntity player) {
            PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(player);
            moodComponent.setMood(1.0f);
            player.playSound(SoundEvents.ENTITY_STRIDER_HAPPY);
            stack.decrement(1);
        }
        return stack;
    }
}
