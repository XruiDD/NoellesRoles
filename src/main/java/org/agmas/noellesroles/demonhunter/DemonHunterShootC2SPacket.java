package org.agmas.noellesroles.demonhunter;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.ShootMuzzleS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.jetbrains.annotations.NotNull;

public record DemonHunterShootC2SPacket(int target) implements CustomPayload {
    public static final Id<DemonHunterShootC2SPacket> ID = new Id<>(Identifier.of(Noellesroles.MOD_ID, "demon_hunter_shoot"));
    public static final PacketCodec<PacketByteBuf, DemonHunterShootC2SPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, DemonHunterShootC2SPacket::target, DemonHunterShootC2SPacket::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<DemonHunterShootC2SPacket> {

        /** 猎魔枪冷却：10 秒（同左轮手枪） */
        private static final int COOLDOWN = GameConstants.getInTicks(0, 10);

        @Override
        public void receive(@NotNull DemonHunterShootC2SPacket payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            ItemStack mainHandStack = player.getMainHandStack();

            // 必须手持猎魔手枪
            if (!mainHandStack.isOf(ModItems.DEMON_HUNTER_PISTOL)) return;
            if (player.getItemCooldownManager().isCoolingDown(mainHandStack.getItem())) return;

            // 播放扳机音
            player.getWorld().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                    WatheSounds.ITEM_REVOLVER_CLICK, SoundCategory.PLAYERS,
                    0.5f, 1f + player.getRandom().nextFloat() * .1f - .05f);

            // 检查子弹
            int bullets = mainHandStack.getOrDefault(ModItems.BULLETS, 0);
            if (bullets <= 0) return;

            // 解析目标
            ServerPlayerEntity target = null;
            if (player.getServerWorld().getEntityById(payload.target()) instanceof ServerPlayerEntity candidate
                    && candidate.distanceTo(player) < 65.0) {
                target = candidate;
            }

            // 记录物品使用
            ServerPlayerEntity recordTarget = target;
            GameRecordManager.recordItemUse(player, Registries.ITEM.getId(mainHandStack.getItem()), recordTarget, null);

            GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());

            boolean shouldKill = false;
            boolean isJesterTarget = false;

            if (target != null) {
                // 检查目标是否处于疯魔
                PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(target);
                boolean targetInFrenzy = psycho.getPsychoTicks() > 0;

                // 检查目标是否为小丑（任何非禁锢状态都可命中）
                if (game.isRole(target, Noellesroles.JESTER)) {
                    JesterPlayerComponent jesterComp = JesterPlayerComponent.KEY.get(target);
                    if (!jesterComp.inStasis) {
                        // 小丑非禁锢 → 有效命中
                        shouldKill = true;
                        isJesterTarget = true;
                    }
                    // 禁锢中 → 无效
                } else if (targetInFrenzy) {
                    // 疯魔中 → 有效命中
                    shouldKill = true;
                }
            }

            // 消耗子弹（无论是否命中有效目标，只要扣了扳机就消耗）
            bullets--;
            if (bullets <= 0) {
                // 子弹用完 → 移除枪
                player.getInventory().removeOne(mainHandStack);
            } else {
                mainHandStack.set(ModItems.BULLETS, bullets);
            }

            if (shouldKill && target != null) {
                // 用猎魔人专属死因，避免触发小丑禁锢（GUN 死因会进入 Jester stasis 分支）
                GameFunctions.killPlayer(target, true, player, Noellesroles.DEATH_REASON_DEMON_HUNTER);

                // 命中小丑 → 自动补一颗子弹
                if (isJesterTarget) {
                    ItemStack pistol = DemonHunterPistolItem.findPistol(player);
                    if (pistol != null) {
                        int currentBullets = pistol.getOrDefault(ModItems.BULLETS, 0);
                        pistol.set(ModItems.BULLETS, currentBullets + 1);
                    }
                    // 如果枪刚被移除（bullets 用完后命中小丑），重新给一把带 1 子弹的
                    else {
                        ItemStack newPistol = new ItemStack(ModItems.DEMON_HUNTER_PISTOL);
                        newPistol.set(ModItems.BULLETS, 1);
                        dev.doctor4t.wathe.util.ShopEntry.insertStackInFreeSlot(player, newPistol);
                    }
                }
            }

            // 播放射击音
            player.getWorld().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                    WatheSounds.ITEM_REVOLVER_SHOOT, SoundCategory.PLAYERS,
                    5f, 1f + player.getRandom().nextFloat() * .1f - .05f);

            // 枪口火光
            for (ServerPlayerEntity tracking : PlayerLookup.tracking(player))
                ServerPlayNetworking.send(tracking, new ShootMuzzleS2CPayload(player.getUuidAsString()));
            ServerPlayNetworking.send(player, new ShootMuzzleS2CPayload(player.getUuidAsString()));

            // 设置冷却
            if (!player.isCreative()) {
                player.getItemCooldownManager().set(ModItems.DEMON_HUNTER_PISTOL, COOLDOWN);
            }
        }
    }
}
