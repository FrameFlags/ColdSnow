package itamio.coldsnow.config;

/** Server-side config, written to {@code config/coldsnow.json}. */
public final class StormConfigData {

    public int schemaVersion = 1;
    public boolean enabled = true;
    public int cycleDaysMin = 14;
    public int cycleDaysMax = 20;
    public long seed = 42L;
    /** Maximum snow depth in layers (vanilla allows at most 8). */
    public int maxLayers = 16;
    public int accumulationIntervalTicks = 20;

    /** Clamps every bounded field in place; never throws. */
    public StormConfigData validate() {
        cycleDaysMin = clamp(cycleDaysMin, 14, 20);
        cycleDaysMax = clamp(cycleDaysMax, 14, 20);
        if (cycleDaysMin > cycleDaysMax) {
            int swap = cycleDaysMin;
            cycleDaysMin = cycleDaysMax;
            cycleDaysMax = swap;
        }
        if (maxLayers < 8) {
            maxLayers = 8;
        } else if (maxLayers > 64) {
            maxLayers = 64;
        }
        return this;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
