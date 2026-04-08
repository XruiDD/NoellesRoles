package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public final class CrosshairTargetHelper {

    private CrosshairTargetHelper() {}

    /**
     * Finds the player closest to the user's crosshair within the given range and minimum alignment.
     *
     * @param user          the player whose crosshair to use
     * @param range         maximum distance in blocks
     * @param minAlignment  minimum dot-product alignment (0.0–1.0, higher = stricter)
     * @return the best-matching player, or null if none found
     */
    @Nullable
    public static PlayerEntity findCrosshairTarget(PlayerEntity user, double range, double minAlignment) {
        Vec3d eyePos = user.getEyePos();
        Vec3d look = user.getRotationVec(1.0F).normalize();

        PlayerEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (PlayerEntity candidate : user.getWorld().getEntitiesByClass(
                PlayerEntity.class,
                user.getBoundingBox().expand(range + 0.5),
                player -> player != user && GameFunctions.isPlayerPlayingAndAlive(player))) {
            if (!user.canSee(candidate)) {
                continue;
            }
            Vec3d toTarget = candidate.getEyePos().subtract(eyePos);
            double distance = toTarget.length();
            if (distance > range || distance <= 0.0001D) {
                continue;
            }
            double alignment = look.dotProduct(toTarget.normalize());
            if (alignment < minAlignment) {
                continue;
            }
            double score = alignment - (distance * 0.01D);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }
}
