package itamio.coldsnow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SnowDepthPolicyTest {

    private final SnowDepthPolicy policy = new SnowDepthPolicy();

    @Test
    void targetLayersTracksIntensityAndClampsToMax() {
        assertEquals(0, policy.targetLayers(snapshot(StormStage.CLEAR, 1.0f), 16));
        assertEquals(4, policy.targetLayers(snapshot(StormStage.LIGHT, 0.25f), 16));
        assertEquals(8, policy.targetLayers(snapshot(StormStage.HEAVY, 0.50f), 16));
        assertEquals(14, policy.targetLayers(snapshot(StormStage.BLIZZARD, 0.85f), 16));
        assertEquals(16, policy.targetLayers(snapshot(StormStage.TAPER, 1.00f), 16));
        assertEquals(0, policy.targetLayers(snapshot(StormStage.TAPER, 0.00f), 16));
    }

    @Test
    void targetLayersNeverLeavesZeroToMaxLayers() {
        assertEquals(0, policy.targetLayers(snapshot(StormStage.LIGHT, -2.0f), 16));
        assertEquals(16, policy.targetLayers(snapshot(StormStage.BLIZZARD, 5.0f), 16));
        assertEquals(0, policy.targetLayers(snapshot(StormStage.BLIZZARD, Float.NaN), 16));
        assertEquals(0, policy.targetLayers(snapshot(StormStage.BLIZZARD, 1.0f), 0));
        assertEquals(2, policy.targetLayers(snapshot(StormStage.BLIZZARD, 1.0f), 2));
        assertEquals(64, policy.targetLayers(snapshot(StormStage.BLIZZARD, 1.0f), 64));
    }

    @Test
    void stepMovesAtMostOneLayerTowardTarget() {
        assertEquals(1, policy.step(3, 5));
        assertEquals(1, policy.step(0, 1));
        assertEquals(-1, policy.step(5, 3));
        assertEquals(-1, policy.step(1, 0));
        assertEquals(0, policy.step(4, 4));
        assertEquals(0, policy.step(0, 0));
        assertTrue(Math.abs(policy.step(0, 64)) <= 1);
    }

    private static StormSnapshot snapshot(StormStage stage, float intensity) {
        return new StormSnapshot(stage, 0, 0L, 0L, 100L, intensity);
    }
}
