package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.entity.HunterTrapEntity;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class HunterTrapItem extends Item {
    private static final String POISONED_KEY = "Poisoned";

    public HunterTrapItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!GameWorldComponent.KEY.get(world).isRole(user, Noellesroles.HUNTER)) {
            return TypedActionResult.pass(stack);
        }

        BlockHitResult hitResult = Item.raycast(world, user, RaycastContext.FluidHandling.NONE);
        if (hitResult.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK) {
            return TypedActionResult.pass(stack);
        }

        BlockPos placePos = hitResult.getBlockPos().up();
        if (!world.getBlockState(placePos.down()).isSolidBlock(world, placePos.down()) || !world.getBlockState(placePos).isAir()) {
            return TypedActionResult.pass(stack);
        }

        if (!world.isClient) {
            this.removeOldestTrapIfNeeded(world, user.getUuid());
            HunterTrapEntity trap = new HunterTrapEntity(NoellesRolesEntities.HUNTER_TRAP_ENTITY, world);
            trap.refreshPositionAndAngles(placePos.getX() + 0.5, placePos.getY() + 0.02, placePos.getZ() + 0.5, 0.0F, 0.0F);
            trap.setOwner(user);
            trap.setPoisoned(isPoisoned(stack));
            world.spawnEntity(trap);
            world.playSound(null, placePos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.PLAYERS, 0.8F, 1.1F);
            if (user instanceof ServerPlayerEntity serverPlayer) {
                NbtCompound extra = new NbtCompound();
                GameRecordManager.putBlockPos(extra, "pos", placePos);
                extra.putString("action", "place");
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), null, extra);
            }
            stack.decrement(1);
        }

        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.hunter_trap.tooltip.line1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.noellesroles.hunter_trap.tooltip.line2").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.noellesroles.hunter_trap.tooltip.line3").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.noellesroles.hunter_trap.tooltip.line4").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.noellesroles.hunter_trap.tooltip.line5").formatted(Formatting.GRAY));
        if (isPoisoned(stack)) {
            tooltip.add(Text.translatable("item.noellesroles.hunter_trap.poisoned").formatted(Formatting.DARK_GREEN));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    public static boolean isPoisoned(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null && customData.copyNbt().getBoolean(POISONED_KEY);
    }

    public static void setPoisoned(ItemStack stack, boolean poisoned) {
        NbtCompound nbt = stack.get(DataComponentTypes.CUSTOM_DATA) != null
            ? stack.get(DataComponentTypes.CUSTOM_DATA).copyNbt()
            : new NbtCompound();
        nbt.putBoolean(POISONED_KEY, poisoned);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private void removeOldestTrapIfNeeded(World world, UUID ownerUuid) {
        Box searchBox = new Box(
            world.getWorldBorder().getBoundWest(),
            world.getBottomY(),
            world.getWorldBorder().getBoundNorth(),
            world.getWorldBorder().getBoundEast(),
            world.getTopY(),
            world.getWorldBorder().getBoundSouth()
        );

        List<HunterTrapEntity> ownedTraps = world.getEntitiesByClass(
            HunterTrapEntity.class,
            searchBox,
            trap -> ownerUuid.equals(trap.getOwnerUuid())
        );

        while (ownedTraps.size() >= 2) {
            HunterTrapEntity oldestTrap = ownedTraps.stream()
                .max(Comparator.comparingInt(trap -> trap.age))
                .orElse(null);
            if (oldestTrap == null) {
                break;
            }
            oldestTrap.discard();
            ownedTraps.remove(oldestTrap);
        }
    }
}
