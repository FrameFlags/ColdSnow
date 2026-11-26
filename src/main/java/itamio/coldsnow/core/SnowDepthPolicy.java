package itamio.coldsnow.core;

/**
 * Maps storm intensity to a snow depth goal and advances one layer at a time.
 * Pure Java: no block or world access.
 */
public final class SnowDepthPolicy {

    /** Target depth in layers, always within {@code [0, maxLayers]}. */
    public int targetLayers(StormSnapshot snapshot, int maxLayers) {
        if (snapshot == null || snapshot.stage() == StormStage.CLEAR || maxLayers <= 0) {
            return 0;
        }
        float intensity = snapshot.intensity();
        if (!Float.isFinite(intensity)) {
            intensity = 0.0f;
        }
        if (intensity < 0.0f) {
            intensity = 0.0f;
        } else if (intensity > 1.0f) {
            intensity = 1.0f;
        }
        int target = Math.round(intensity * (float) maxLayers);
        return Math.max(0, Math.min(maxLayers, target));
    }

    /** One-layer step toward the target: -1, 0 or +1. */
    public int step(int currentLayers, int targetLayers) {
        return Integer.compare(targetLayers, currentLayers);
    }
}
