package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.UUID;

public class AntidoteItem extends Item {
    // Charge time: 1.5 seconds = 30 ticks
    private static final int USE_TIME = 30;
    // Cooldown after use: 5 minutes = 5 * 60 * 20 ticks
    private static final int COOLDOWN_TICKS = 5 * 60 * 20;
    // Initial cooldown at game start: 2 minutes = 2 * 60 * 20 ticks
    public static final int INITIAL_COOLDOWN_TICKS = 2 * 60 * 20;
    // Max distance to target: 3 blocks
    private static final double MAX_DISTANCE_SQ = 9.0;

    public AntidoteItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return ActionResult.FAIL;
        }

        // Target must be a player
        if (!(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }

        // Target must be alive
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
            return ActionResult.PASS;
        }

        // Check if target is poisoned
        PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(target);
        if (poisonComp.poisonTicks <= 0) {
            return ActionResult.PASS;
        }

        // Store target UUID in item stack for later use
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("target", target.getUuid());
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        // Start the use action (arm-raising animation)
        user.setCurrentHand(hand);

        return ActionResult.CONSUME;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient) return;

        // Get stored target
        UUID targetUuid = getTargetUuid(stack);
        if (targetUuid == null) {
            user.stopUsingItem();
            return;
        }

        // Check if target still exists and in range
        PlayerEntity target = world.getPlayerByUuid(targetUuid);
        if (target == null || user.squaredDistanceTo(target) > MAX_DISTANCE_SQ) {
            user.stopUsingItem();
            clearTargetUuid(stack);
            return;
        }

        // Check if target is still poisoned
        PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(target);
        if (poisonComp.poisonTicks <= 0) {
            user.stopUsingItem();
            clearTargetUuid(stack);
            return;
        }
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (world.isClient) {
            return stack;
        }

        if (!(user instanceof PlayerEntity player)) {
            return stack;
        }

        // Get stored target
        UUID targetUuid = getTargetUuid(stack);
        if (targetUuid == null) {
            return stack;
        }

        PlayerEntity target = world.getPlayerByUuid(targetUuid);
        if (target == null) {
            clearTargetUuid(stack);
            return stack;
        }

        // Final range check
        if (user.squaredDistanceTo(target) > MAX_DISTANCE_SQ) {
            clearTargetUuid(stack);
            return stack;
        }

        // Cure the poison
        PlayerPoisonComponent poisonComp = PlayerPoisonComponent.KEY.get(target);
        if (poisonComp.poisonTicks > 0) {
            poisonComp.reset();

            // Play success sound
            world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.2F);

            // Set cooldown
            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
        }

        // Clear target data
        clearTargetUuid(stack);

        return stack;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        // Clear target data when use is interrupted
        clearTargetUuid(stack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return USE_TIME;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }

    private UUID getTargetUuid(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.containsUuid("target")) {
                return nbt.getUuid("target");
            }
        }
        return null;
    }

    private void clearTargetUuid(ItemStack stack) {
        stack.remove(DataComponentTypes.CUSTOM_DATA);
    }
}
