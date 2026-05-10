package org.agmas.noellesroles.client.murdermayhem;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.murdermayhem.FogOfWarMurderMayhemEvent;
import org.agmas.noellesroles.murdermayhem.MurderMayhemWorldComponent;
import org.agmas.noellesroles.util.SpectatorStateHelper;

public final class FogOfWarClientHelper {
    private static final float RENDER_RADIUS_STEP_PER_TICK = 0.05F;
    private static boolean cachedFogActive;
    private static boolean cachedIgnoreFog;
    private static int cachedFogRadius = FogOfWarMurderMayhemEvent.INITIAL_FOG_RADIUS;
    private static float previousRenderedFogRadius = FogOfWarMurderMayhemEvent.INITIAL_FOG_RADIUS;
    private static float renderedFogRadius = FogOfWarMurderMayhemEvent.INITIAL_FOG_RADIUS;

    private FogOfWarClientHelper() {
    }

    public static void tickClientState(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        World world = client.world;
        if (player == null || world == null) {
            cachedFogActive = false;
            cachedIgnoreFog = true;
            cachedFogRadius = FogOfWarMurderMayhemEvent.INITIAL_FOG_RADIUS;
            previousRenderedFogRadius = FogOfWarMurderMayhemEvent.INITIAL_FOG_RADIUS;
            renderedFogRadius = FogOfWarMurderMayhemEvent.INITIAL_FOG_RADIUS;
            return;
        }

        cachedFogActive = isFogOfWarActive(world);
        cachedIgnoreFog = ignoresFog(player);
        cachedFogRadius = getFogRadius(world);
        previousRenderedFogRadius = renderedFogRadius;
        renderedFogRadius = approachRenderedRadius(renderedFogRadius, getVisualFogRadius(cachedFogRadius));
    }

    public static boolean isCachedFogActive() {
        return cachedFogActive;
    }

    public static boolean isCachedIgnoreFog() {
        return cachedIgnoreFog;
    }

    public static int getCachedFogRadius() {
        return cachedFogRadius;
    }

    public static float getRenderedFogRadius(float tickDelta) {
        return MathHelper.lerp(MathHelper.clamp(tickDelta, 0.0F, 1.0F), previousRenderedFogRadius, renderedFogRadius);
    }

    public static float getRenderedFogRadius() {
        return renderedFogRadius;
    }

    public static boolean isFogOfWarActive(World world) {
        if (world == null) {
            return false;
        }
        MurderMayhemWorldComponent component = MurderMayhemWorldComponent.KEY.get(world);
        Identifier currentEventId = component.getCurrentEventId();
        return FogOfWarMurderMayhemEvent.ID.equals(currentEventId);
    }

    public static boolean ignoresFog(PlayerEntity player) {
        return player == null || SpectatorStateHelper.isSpectatorLike(player);
    }

    public static int getFogRadius(World world) {
        if (!isFogOfWarActive(world)) {
            return FogOfWarMurderMayhemEvent.INITIAL_FOG_RADIUS;
        }
        int syncedRadius = MurderMayhemWorldComponent.KEY.get(world).getFogRadius();
        if (syncedRadius < 0) {
            return FogOfWarMurderMayhemEvent.INITIAL_FOG_RADIUS;
        }
        return MathHelper.clamp(
                syncedRadius,
                FogOfWarMurderMayhemEvent.MIN_FOG_RADIUS,
                FogOfWarMurderMayhemEvent.MAX_FOG_RADIUS
        );
    }

    public static float getFogShellThickness() {
        return 5.0F;
    }

    public static int getFogColor() {
        return 0xFFB8B8B4;
    }

    public static Vec3d getFogColorVec() {
        return new Vec3d(184.0 / 255.0, 184.0 / 255.0, 180.0 / 255.0);
    }

    public static boolean isTargetInsideFog(PlayerEntity viewer, PlayerEntity target) {
        if (viewer == null || target == null) {
            return false;
        }
        if (viewer == target || ignoresFog(viewer) || ignoresFog(target)) {
            return false;
        }
        if (!isFogOfWarActive(viewer.getWorld())) {
            return false;
        }
        double fogRadius = getRenderedFogRadius();
        return viewer.squaredDistanceTo(target) > fogRadius * fogRadius;
    }

    public static double getFogDistance(PlayerEntity viewer, PlayerEntity target) {
        if (viewer == null || target == null) {
            return 0.0;
        }
        return Math.max(0.0, Math.sqrt(viewer.squaredDistanceTo(target)) - getRenderedFogRadius());
    }

    public static boolean shouldClampKillerInstinct(PlayerEntity viewer, PlayerEntity target) {
        if (viewer == null || target == null || ignoresFog(viewer)) {
            return false;
        }
        if (!isFogOfWarActive(viewer.getWorld())) {
            return false;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
        return gameWorld.canUseKillerFeatures(viewer) && !ignoresFog(target);
    }

    public static boolean isWithinKillerInstinctLimit(PlayerEntity viewer, PlayerEntity target) {
        double maxDistance = getEffectiveKillerInstinctFogRadius(viewer.getWorld()) + FogOfWarMurderMayhemEvent.INSTINCT_EXTRA_DISTANCE;
        return viewer.squaredDistanceTo(target) <= maxDistance * maxDistance;
    }

    public static boolean shouldBlockKillerInstinctEntity(PlayerEntity viewer, Entity target) {
        if (viewer == null || target == null || viewer == target || ignoresFog(viewer)) {
            return false;
        }
        if (!isFogOfWarActive(viewer.getWorld())) {
            return false;
        }
        if (target instanceof PlayerEntity playerTarget) {
            return shouldClampKillerInstinct(viewer, playerTarget) && !isWithinKillerInstinctLimit(viewer, playerTarget);
        }
        if (!GameWorldComponent.KEY.get(viewer.getWorld()).canUseKillerFeatures(viewer)) {
            return false;
        }
        double maxDistance = getEffectiveKillerInstinctFogRadius(viewer.getWorld()) + FogOfWarMurderMayhemEvent.INSTINCT_EXTRA_DISTANCE;
        return viewer.squaredDistanceTo(target) > maxDistance * maxDistance;
    }

    private static float getEffectiveKillerInstinctFogRadius(World world) {
        return getVisualFogRadius(getFogRadius(world));
    }

    public static float getFogBlendProgress(PlayerEntity viewer, PlayerEntity target) {
        if (!isTargetInsideFog(viewer, target)) {
            return 0.0F;
        }
        return MathHelper.clamp((float) (getFogDistance(viewer, target) / getFogShellThickness()), 0.0F, 1.0F);
    }

    public static float getShadowAlpha(PlayerEntity viewer, PlayerEntity target) {
        if (!isTargetInsideFog(viewer, target)) {
            return 1.0F;
        }
        return MathHelper.lerp(getFogBlendProgress(viewer, target), 1.0F, 0.65F);
    }

    public static Vec3d getShadowColor(PlayerEntity viewer, PlayerEntity target) {
        float progress = getFogBlendProgress(viewer, target);
        Vec3d fogColor = getFogColorVec();
        return new Vec3d(
                MathHelper.lerp(progress, 0.0F, (float) fogColor.x),
                MathHelper.lerp(progress, 0.0F, (float) fogColor.y),
                MathHelper.lerp(progress, 0.0F, (float) fogColor.z)
        );
    }

    public static ClientPlayerEntity getLocalPlayer() {
        return MinecraftClient.getInstance().player;
    }

    public static boolean isLocalFogViewerActive() {
        ClientPlayerEntity player = getLocalPlayer();
        return player != null && !ignoresFog(player) && isFogOfWarActive(player.getWorld()) && WatheClient.isPlayerPlayingAndAlive();
    }

    private static float approachRenderedRadius(float current, float target) {
        float delta = target - current;
        if (Math.abs(delta) <= RENDER_RADIUS_STEP_PER_TICK) {
            return target;
        }
        return current + Math.copySign(RENDER_RADIUS_STEP_PER_TICK, delta);
    }

    private static float getVisualFogRadius(int logicalRadius) {
        return logicalRadius <= 0 ? FogOfWarMurderMayhemEvent.INITIAL_FOG_RADIUS : logicalRadius;
    }
}
