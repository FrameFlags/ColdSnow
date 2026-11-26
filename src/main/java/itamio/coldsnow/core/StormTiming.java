package itamio.coldsnow.core;

/**
 * Immutable timing parameters for the storm timeline.
 *
 * @param minDays shortest possible cycle, in Minecraft days
 * @param maxDays longest possible cycle, in Minecraft days
 * @param seed    deterministic cycle-length seed
 */
public record StormTiming(int minDays, int maxDays, long seed) {

    public static StormTiming defaults() {
        return new StormTiming(14, 20, 42L);
    }

    /** Keep {@code minDays} positive and never greater than {@code maxDays}. */
    public StormTiming {
        minDays = Math.max(1, Math.min(minDays, maxDays));
    }
}
