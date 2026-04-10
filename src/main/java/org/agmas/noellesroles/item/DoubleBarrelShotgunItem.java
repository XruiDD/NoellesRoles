package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.Item;
import net.minecraft.inventory.StackReference;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;

import java.util.List;

public class DoubleBarrelShotgunItem extends Item {
    public static final int MAX_SHELLS = 2;
    public static final int FIRE_COOLDOWN_TICKS = 12;
    public static final int EMPTY_COOLDOWN_TICKS = 20 * 30;
    public static final int RELOAD_WINDOW_TICKS = 20 * 10;
    public static final double RANGE = 8.0;
    private static final String LOADED_SHELLS_KEY = "LoadedShells";
    private static final String RELOAD_WINDOW_UNTIL_KEY = "ReloadWindowUntil";

    public DoubleBarrelShotgunItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!GameWorldComponent.KEY.get(world).isRole(user, Noellesroles.HUNTER)) {
            return TypedActionResult.pass(stack);
        }
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }

        int loaded = getLoadedShells(stack);
        if (loaded <= 0) {
            if (!world.isClient) {
                world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.8F, 1.0F);
            }
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            int remainingShells = loaded - 1;
            NbtCompound nbt = getOrCreateCustomData(stack);
            nbt.putInt(LOADED_SHELLS_KEY, Math.max(0, remainingShells));
            nbt.remove(RELOAD_WINDOW_UNTIL_KEY);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            PlayerEntity target = findTarget(user);
            if (target instanceof ServerPlayerEntity serverTarget && user instanceof ServerPlayerEntity serverUser) {
                GameFunctions.killPlayer(serverTarget, true, serverUser, GameConstants.DeathReasons.GUN);
            }
            if (user instanceof ServerPlayerEntity serverUser) {
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "fire");
                extra.putInt("remaining_shells", remainingShells);
                extra.putBoolean("hit", target instanceof ServerPlayerEntity);
                GameRecordManager.recordItemUse(
                    serverUser,
                    Registries.ITEM.getId(this),
                    target instanceof ServerPlayerEntity serverTarget ? serverTarget : null,
                    extra
                );
            }
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.6F, 1.7F);
            user.getItemCooldownManager().set(this, remainingShells <= 0 ? EMPTY_COOLDOWN_TICKS : FIRE_COOLDOWN_TICKS);
        }

        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT) {
            return false;
        }
        if (!otherStack.isOf(org.agmas.noellesroles.ModItems.DOUBLE_BARREL_SHELL)) {
            return false;
        }
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.HUNTER)) {
            return false;
        }
        if (player.getItemCooldownManager().isCoolingDown(this) || !canReload(player, stack)) {
            return false;
        }

        int loaded = getLoadedShells(stack);
        if (loaded >= MAX_SHELLS) {
            return false;
        }

        setLoadedShells(stack, loaded + 1);
        if (loaded == 0) {
            setReloadWindowUntil(stack, player.getWorld().getTime() + RELOAD_WINDOW_TICKS);
        }
        if (!player.isCreative()) {
            otherStack.decrement(1);
            cursorStackReference.set(otherStack);
        }

        if (!player.getWorld().isClient) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                NbtCompound extra = new NbtCompound();
                extra.putString("action", "reload");
                extra.putInt("loaded_shells", loaded + 1);
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), null, extra);
            }
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value(), SoundCategory.PLAYERS, 0.6F, 1.4F);
        }
        return true;
    }

    private PlayerEntity findTarget(PlayerEntity user) {
        Vec3d eyePos = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0F);
        Vec3d end = eyePos.add(look.multiply(RANGE));
        Box searchBox = user.getBoundingBox().stretch(look.multiply(RANGE)).expand(0.5);

        return user.getWorld()
            .getEntitiesByClass(PlayerEntity.class, searchBox, candidate ->
                candidate != user && GameFunctions.isPlayerAliveAndSurvival(candidate))
            .stream()
            .filter(candidate -> {
                var hit = candidate.getBoundingBox().expand(0.1).raycast(eyePos, end);
                if (hit.isEmpty()) {
                    return false;
                }

                Vec3d hitPos = hit.get();

                HitResult blockHit = user.getWorld().raycast(new RaycastContext(
                    eyePos,
                    hitPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    user
                ));
                return blockHit.getType() == HitResult.Type.MISS
                    || blockHit.getPos().squaredDistanceTo(eyePos) + 1.0E-4 >= eyePos.squaredDistanceTo(hitPos);
            })
            .min(java.util.Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(user)))
            .orElse(null);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.double_barrel_shotgun.tooltip.line1", getLoadedShells(stack)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.noellesroles.double_barrel_shotgun.tooltip.line2").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.noellesroles.double_barrel_shotgun.tooltip.line3").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.noellesroles.double_barrel_shotgun.tooltip.line4").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.noellesroles.double_barrel_shotgun.tooltip.line5").formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }

    public static int getLoadedShells(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }
        return customData.copyNbt().getInt(LOADED_SHELLS_KEY);
    }

    public static void setLoadedShells(ItemStack stack, int shells) {
        NbtCompound nbt = getOrCreateCustomData(stack);
        nbt.putInt(LOADED_SHELLS_KEY, Math.max(0, Math.min(MAX_SHELLS, shells)));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private static boolean canReload(PlayerEntity player, ItemStack stack) {
        int loaded = getLoadedShells(stack);
        if (loaded <= 0) {
            return true;
        }
        return player.getWorld().getTime() <= getReloadWindowUntil(stack);
    }

    private static long getReloadWindowUntil(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return 0L;
        }
        return customData.copyNbt().getLong(RELOAD_WINDOW_UNTIL_KEY);
    }

    private static void setReloadWindowUntil(ItemStack stack, long worldTime) {
        NbtCompound nbt = getOrCreateCustomData(stack);
        nbt.putLong(RELOAD_WINDOW_UNTIL_KEY, worldTime);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private static void clearReloadWindow(ItemStack stack) {
        NbtCompound nbt = getOrCreateCustomData(stack);
        nbt.remove(RELOAD_WINDOW_UNTIL_KEY);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private static NbtCompound getOrCreateCustomData(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null ? customData.copyNbt() : new NbtCompound();
    }
}
