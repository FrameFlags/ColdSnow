package itamio.coldsnow.client;

import itamio.coldsnow.core.StormStage;

/**
 * Pure, deterministic stage/intensity/density to flakes-per-tick mapping.
 * No world access, no randomness, no mutable state — safe to unit-test in isolation.
 */
public final class ParticleSpawn {
    /** Safety ceiling so a maximum-density blizzard cannot flood the particle engine. */
    public static final int MAX_FLAKES_PER_TICK = 40;

    private ParticleSpawn() {
    }

    /**
     * @param stage            storm stage; {@code null} and {@code CLEAR} yield zero
     * @param intensity        stage intensity, clamped to {@code [0,1]}
     * @param densityMultiplier user density setting, clamped to {@code [0,2]}
     * @return number of flakes to spawn this tick ({@code 0..MAX_FLAKES_PER_TICK})
     */
    public static int flakesFor(StormStage stage, float intensity, float densityMultiplier) {
        if (stage == null || stage == StormStage.CLEAR) {
            return 0;
        }
        float base = switch (stage) {
            case LIGHT -> 6f;
            case HEAVY -> 12f;
            case BLIZZARD -> 24f;
            case TAPER -> 8f;
            case CLEAR -> 0f;
        };
        float scaledIntensity = Float.isFinite(intensity)
                ? Math.max(0f, Math.min(1f, intensity))
                : 0f;
        float density = Float.isFinite(densityMultiplier)
                ? Math.max(0f, Math.min(2f, densityMultiplier))
                : 1f;
        return Math.min(MAX_FLAKES_PER_TICK, Math.round(base * scaledIntensity * density));
    }
}
