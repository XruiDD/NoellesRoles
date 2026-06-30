package org.agmas.noellesroles.shadowjester;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.music.MusicMomentType;
import org.agmas.noellesroles.music.WorldMusicComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 影子小丑玩家组件。
 * <p>影子小丑成对生成。做完四个任务获得短刀后有两条路线：
 * <ul>
 *   <li><b>背叛</b>：捅死搭档 → 化身真正的小丑（保留的刀失去杀伤力 {@link #betrayalTrophy}）。</li>
 *   <li><b>结盟</b>：双方按 G 互相确认 → 命运绑定（一死俱亡）。先发起方=透视位（{@link #instinctVision}、刀被没收），
 *       后同意方=输出位（{@link #realKnife} 真刀 + 德加林）。当杀手杀光平民、本应判杀手胜利时，进入
 *       「双影谢幕」终局对决时刻（{@link #showdownActive}），二人组与杀手决战，活下来的一方胜利。</li>
 * </ul>
 */
public class ShadowJesterPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<ShadowJesterPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "shadow_jester"), ShadowJesterPlayerComponent.class);

    /** 开局立即注入的任务数（其余由心情系统随时间自动发放） */
    public static final int INITIAL_TASKS = 3;
    /** 取得短刀需要完成的任务总数：第 4 个由心情系统自动发放，借此拖延出刀时间 */
    public static final int TASKS_REQUIRED = 4;

    private final PlayerEntity player;

    private UUID partnerUuid;
    private int tasksCompleted;
    private boolean knifeGiven;

    private boolean allyProposed;   // 我已按 G 发起结盟（单次）
    private boolean allied;         // 已命运绑定
    private boolean realKnife;      // 刀可杀任何人（输出位 / 终局对决）
    private boolean instinctVision; // 按本能键(Alt)透视所有存活玩家（透视位 / 终局对决）
    private boolean betrayalTrophy; // 背叛变身后保留的废刀标记（使刀失去杀伤力）
    private boolean showdownActive; // 已进入终局对决时刻（防重复触发）

    public ShadowJesterPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void setPartner(UUID uuid) {
        this.partnerUuid = uuid;
        this.sync();
    }

    public UUID getPartnerUuid() {
        return partnerUuid;
    }

    public int incrementTasksCompleted() {
        return ++this.tasksCompleted;
    }

    public int getTasksCompleted() {
        return tasksCompleted;
    }

    public boolean isKnifeGiven() {
        return knifeGiven;
    }

    public void setKnifeGiven(boolean knifeGiven) {
        this.knifeGiven = knifeGiven;
        this.sync();
    }

    public boolean isAllyProposed() {
        return allyProposed;
    }

    public void setAllyProposed(boolean allyProposed) {
        this.allyProposed = allyProposed;
        this.sync();
    }

    public boolean isAllied() {
        return allied;
    }

    public void setAllied(boolean allied) {
        this.allied = allied;
        this.sync();
    }

    public boolean isRealKnife() {
        return realKnife;
    }

    public void setRealKnife(boolean realKnife) {
        this.realKnife = realKnife;
        this.sync();
    }

    public boolean hasInstinctVision() {
        return instinctVision;
    }

    public void setInstinctVision(boolean instinctVision) {
        this.instinctVision = instinctVision;
        this.sync();
    }

    public boolean isBetrayalTrophy() {
        return betrayalTrophy;
    }

    public boolean isShowdownActive() {
        return showdownActive;
    }

    public void setShowdownActive(boolean showdownActive) {
        this.showdownActive = showdownActive;
        this.sync();
    }

    public void reset() {
        this.partnerUuid = null;
        this.tasksCompleted = 0;
        this.knifeGiven = false;
        this.allyProposed = false;
        this.allied = false;
        this.realKnife = false;
        this.instinctVision = false;
        this.betrayalTrophy = false;
        this.showdownActive = false;
        this.sync();
    }

    /**
     * 背叛变身：搭档死亡后本人化身真正的小丑。清空结盟相关状态，但保留 {@link #betrayalTrophy} 标记，
     * 使保留下来的刀依旧无法造成伤害（与捅人前的限制一致）。
     */
    public void markBetrayalTrophy() {
        this.partnerUuid = null;
        this.allyProposed = false;
        this.allied = false;
        this.realKnife = false;
        this.instinctVision = false;
        this.showdownActive = false;
        this.betrayalTrophy = true;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (partnerUuid != null) tag.putUuid("partner", partnerUuid);
        tag.putInt("tasksCompleted", tasksCompleted);
        tag.putBoolean("knifeGiven", knifeGiven);
        tag.putBoolean("allyProposed", allyProposed);
        tag.putBoolean("allied", allied);
        tag.putBoolean("realKnife", realKnife);
        tag.putBoolean("instinctVision", instinctVision);
        tag.putBoolean("betrayalTrophy", betrayalTrophy);
        tag.putBoolean("showdownActive", showdownActive);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        partnerUuid = tag.containsUuid("partner") ? tag.getUuid("partner") : null;
        tasksCompleted = tag.getInt("tasksCompleted");
        knifeGiven = tag.getBoolean("knifeGiven");
        allyProposed = tag.getBoolean("allyProposed");
        allied = tag.getBoolean("allied");
        realKnife = tag.getBoolean("realKnife");
        instinctVision = tag.getBoolean("instinctVision");
        betrayalTrophy = tag.getBoolean("betrayalTrophy");
        showdownActive = tag.getBoolean("showdownActive");
    }

    /**
     * 开局立即给影子小丑刷三个不同的任务（FAKE 角色心情恒满，任务不掉心情）。
     * 直接写入 {@link PlayerMoodComponent#tasks}，之后由心情组件照常检测完成。
     */
    public static void injectInitialTasks(PlayerEntity player) {
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        mood.tasks.clear();
        List<PlayerMoodComponent.Task> types = new ArrayList<>(List.of(PlayerMoodComponent.Task.values()));
        // 从全部任务类型里随机移除，直到剩下 INITIAL_TASKS 个不同类型（其余留给心情系统自动发放）
        while (types.size() > INITIAL_TASKS) {
            types.remove(player.getRandom().nextInt(types.size()));
        }
        for (PlayerMoodComponent.Task type : types) {
            mood.tasks.put(type, createTask(type));
        }
        mood.sync();
    }

    private static PlayerMoodComponent.TrainTask createTask(PlayerMoodComponent.Task type) {
        return switch (type) {
            case SLEEP -> new PlayerMoodComponent.SleepTask(GameConstants.SLEEP_TASK_DURATION);
            case OUTSIDE -> new PlayerMoodComponent.OutsideTask(GameConstants.OUTSIDE_TASK_DURATION);
            case EAT -> new PlayerMoodComponent.EatTask();
            case DRINK -> new PlayerMoodComponent.DrinkTask();
        };
    }

    /**
     * 激活「双影谢幕」终局对决时刻（仅调用一次）：给绑定的二人组都补齐 真刀 + 德加林 + 透视，
     * 全场 title/副标题 + 复用小丑时刻 BGM。物品发放前先检测，避免刀/枪在背包里出现两把。
     */
    public static void activateShowdown(ServerWorld world, List<ServerPlayerEntity> boundJesters) {
        for (ServerPlayerEntity p : boundJesters) {
            ShadowJesterPlayerComponent comp = KEY.get(p);
            comp.realKnife = true;
            comp.instinctVision = true;
            comp.showdownActive = true;
            comp.sync();
            // 透视位之前被没收了刀，补一把（真刀）；输出位已有则不重复发
            if (!p.getInventory().contains(s -> s.isOf(WatheItems.KNIFE))) {
                p.giveItemStack(new ItemStack(WatheItems.KNIFE));
            }
            // 透视位之前没有德加林，补一把；输出位已有则不重复发
            if (!p.getInventory().contains(s -> s.isOf(WatheItems.DERRINGER))) {
                p.giveItemStack(WatheItems.DERRINGER.getDefaultStack());
            }
        }
        // 全场 title + 副标题 + 复用小丑时刻 BGM
        WorldMusicComponent.KEY.get(world).startMusic(MusicMomentType.JESTER_MOMENT, 1);
        for (ServerPlayerEntity sp : world.getPlayers()) {
            sp.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.translatable("title.noellesroles.shadow_jester_showdown").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)));
            sp.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.translatable("subtitle.noellesroles.shadow_jester_showdown")));
            sp.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 100, 10));
        }
    }
}
