package org.agmas.noellesroles.demonhunter;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 猎魔手枪 — 复用德加林手枪的材质，独立物品 ID。
 * <p>
 * 客户端射击逻辑（发包 + 粒子）由 {@code DemonHunterClientHelper} 在 client source set 注册。
 * 本类仅包含通用（common）逻辑。
 */
public class DemonHunterPistolItem extends Item {

    /** 客户端射击回调，由 client source set 在初始化时注入 */
    public static ClientShootHandler clientShootHandler = null;

    public DemonHunterPistolItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        int bullets = stack.getOrDefault(ModItems.BULLETS, 0);

        if (world.isClient && clientShootHandler != null) {
            if (user.getItemCooldownManager().isCoolingDown(this)) {
                return TypedActionResult.pass(stack);
            }
            clientShootHandler.shoot(user, bullets);
        }
        return TypedActionResult.consume(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tip.noellesroles.demon_hunter_pistol.usage"));
        tooltip.add(Text.translatable("tip.noellesroles.demon_hunter_pistol.target"));
        tooltip.add(Text.translatable("tip.noellesroles.demon_hunter_pistol.jester"));
        tooltip.add(Text.translatable("tip.noellesroles.demon_hunter_pistol.expire"));
        tooltip.add(Text.empty());
        int bullets = stack.getOrDefault(ModItems.BULLETS, 0);
        tooltip.add(Text.translatable("tip.noellesroles.demon_hunter_pistol.bullets", bullets));
    }

    // ── 射击辅助（server/common） ──

    public static HitResult getGunTarget(PlayerEntity user) {
        return ProjectileUtil.getCollision(user,
                entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player),
                5f);
    }

    public static int resolveTargetFromHitResult(World world, HitResult collision) {
        return RevolverItem.resolveTargetFromHitResult(world, collision);
    }

    // ── 工具方法 ──

    public static void removePistol(PlayerEntity player) {
        player.getInventory().remove(
                s -> s.isOf(ModItems.DEMON_HUNTER_PISTOL),
                Integer.MAX_VALUE,
                player.playerScreenHandler.getCraftingInput());
    }

    public static ItemStack findPistol(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ModItems.DEMON_HUNTER_PISTOL)) return stack;
        }
        return null;
    }

    /** 客户端射击回调接口 */
    @FunctionalInterface
    public interface ClientShootHandler {
        void shoot(PlayerEntity user, int bullets);
    }
}
