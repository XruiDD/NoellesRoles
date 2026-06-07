package org.agmas.noellesroles.client.mixin.jester;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import org.agmas.noellesroles.client.jester.JesterMomentClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 小丑时刻：抑制本地玩家"低 san 疯魔随机物品(psychosisItems)"的生成，
 * 避免它覆盖小丑覆盖层（球棒视觉、握姿、拿枪臂姿三处都读这个 map）。
 * 名字乱码依赖 mood 值(isLowerThanMid)，与此 map 无关，故不受影响。
 * 对小丑本人/死亡旁观者同样清空 → 他们看到真实物品（与"看真实世界"一致）。
 */
@Mixin(PlayerMoodComponent.class)
public class JesterMomentPsychosisSuppressMixin {

    @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true)
    private void noellesroles$suppressPsychosis(CallbackInfo ci) {
        if (JesterMomentClient.isActive()) {
            ((PlayerMoodComponent) (Object) this).getPsychosisItems().clear();
            ci.cancel();
        }
    }
}
