package org.agmas.noellesroles.util;

import com.mojang.datafixers.util.Pair;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 隐藏装备工具类
 */
public final class HiddenEquipmentHelper {

    private HiddenEquipmentHelper() {}

    /**
     * 检查物品是否应该被隐藏
     */
    public static boolean shouldHideItem(ItemStack stack, PlayerEntity holder) {
        if (stack.isEmpty()) return false;

        if (stack.isOf(ModItems.DEFENSE_VIAL)) return true;
        if (stack.isOf(ModItems.NEUTRAL_MASTER_KEY)) return true;
        if (stack.isOf(ModItems.ANTIDOTE)) return true;
        if (stack.isOf(ModItems.IRON_MAN_VIAL)) return true;
        if (stack.isOf(Items.WRITTEN_BOOK)) return true;

        if (stack.isOf(ModItems.TIMED_BOMB)) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(holder.getWorld());
            return gameWorld.isRole(holder, Noellesroles.BOMBER) && !BomberPlayerComponent.KEY.get(holder).hasBomb();
        }

        // 酒保商店物品和维修工具 — 始终隐藏
        if (stack.isOf(ModItems.RUM)) return true;
        if (stack.isOf(ModItems.GIN)) return true;
        if (stack.isOf(ModItems.VODKA)) return true;
        if (stack.isOf(ModItems.TEQUILA)) return true;
        if (stack.isOf(ModItems.WHISKEY)) return true;
        if (stack.isOf(ModItems.ICE_CUBE)) return true;
        if (stack.isOf(ModItems.LIQUEUR)) return true;
        if (stack.isOf(ModItems.SPECIAL_SPICE)) return true;
        if (stack.isOf(ModItems.REPAIR_TOOL)) return true;

        // 基酒瓶 — 酒保手中隐藏，其他角色手中可见
        if (stack.isOf(ModItems.BASE_SPIRIT)) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(holder.getWorld());
            return gameWorld.isRole(holder, Noellesroles.BARTENDER);
        }

        return false;
    }

    /**
     * 过滤装备数据包，将需要隐藏的物品替换为空
     *
     * @param packet   原始数据包
     * @param holder   物品持有者
     * @param observer 观察者（接收数据包的玩家）
     * @return 修改后的数据包，如果无需修改则返回 null
     */
    public static EntityEquipmentUpdateS2CPacket filterPacket(
            EntityEquipmentUpdateS2CPacket packet,
            PlayerEntity holder,
            ServerPlayerEntity observer
    ) {
        // 只对活着的生存模式玩家隐藏
        if (!GameFunctions.isPlayerPlayingAndAlive(observer)) {
            return null;
        }

        List<Pair<EquipmentSlot, ItemStack>> original = packet.getEquipmentList();
        List<Pair<EquipmentSlot, ItemStack>> filtered = new ArrayList<>(original.size());
        boolean modified = false;

        for (Pair<EquipmentSlot, ItemStack> pair : original) {
            EquipmentSlot slot = pair.getFirst();
            ItemStack stack = pair.getSecond();

            if ((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)
                    && shouldHideItem(stack, holder)) {
                filtered.add(Pair.of(slot, ItemStack.EMPTY));
                modified = true;
            } else {
                filtered.add(pair);
            }
        }

        return modified ? new EntityEquipmentUpdateS2CPacket(packet.getEntityId(), filtered) : null;
    }
}
