package itamio.coldsnow.core;

import java.util.Objects;

/**
 * Pure, deterministic storm timeline. Given a {@link StormTiming} and a world time,
 * {@link #at(long)} always returns the same {@link StormSnapshot}; no world or clock
 * state is consulted.
 *
 * <p>Cycle lengths vary per cycle, so the containing cycle is located by a bounded
 * search starting from an average-length estimate. Division is only ever performed
 * by the (constant) average length, never by an individual cycle length.</p>
 */
public final class StormTimeline {

    public static final long DAY_TICKS = 24000L;

    /** Guards against int overflow and excessively long prefix sums for absurd inputs. */
    private static final int MAX_GUESS_CYCLE = 1_000_000;
    private static final int MAX_ADJUSTMENTS = 128;

    private final StormTiming timing;

    public StormTimeline(StormTiming timing) {
        this.timing = Objects.requireNonNull(timing, "timing");
    }

    /** Length of cycle {@code cycleIndex} in ticks: days in [minDays, maxDays] times 24000. */
    public long cycleLengthTicks(int cycleIndex) {
        int span = Math.max(1, timing.maxDays() - timing.minDays() + 1);
        int days = timing.minDays() + (hash(timing.seed(), cycleIndex) % span);
        return (long) days * DAY_TICKS;
    }

    /** First tick of cycle {@code cycleIndex}; negative indices count backwards from 0. */
    public long cycleStartTicks(int cycleIndex) {
        long start = 0L;
        if (cycleIndex > 0) {
            for (int i = 0; i < cycleIndex; i++) {
                start += cycleLengthTicks(i);
            }
        } else {
            for (int i = -1; i >= cycleIndex; i--) {
                start -= cycleLengthTicks(i);
            }
        }
        return start;
    }

    /**
     * Snapshot at an arbitrary (possibly negative) world time.
     * Pure function of {@code (timing, dayTimeTicks)}.
     */
    public StormSnapshot at(long dayTimeTicks) {
        long averageTicks = ((long) timing.minDays() + timing.maxDays()) * DAY_TICKS / 2L;
        if (averageTicks < DAY_TICKS) {
            averageTicks = DAY_TICKS;
        }

        long guess = Math.floorDiv(dayTimeTicks, averageTicks);
        int index = (int) Math.max(-MAX_GUESS_CYCLE, Math.min((long) MAX_GUESS_CYCLE, guess));

        // Prefix sum once, then adjust incrementally so each step is O(1).
        long start = cycleStartTicks(index);
        int adjustments = 0;

        while (start > dayTimeTicks && adjustments < MAX_ADJUSTMENTS) {
            index--;
            start -= cycleLengthTicks(index);
            adjustments++;
        }
        while (start + cycleLengthTicks(index) <= dayTimeTicks && adjustments < MAX_ADJUSTMENTS) {
            start += cycleLengthTicks(index);
            index++;
            adjustments++;
        }

        long length = cycleLengthTicks(index);
        long tickInCycle = dayTimeTicks - start;
        if (tickInCycle < 0L) {
            tickInCycle = 0L;
        } else if (tickInCycle >= length) {
            tickInCycle = length - 1L;
        }

        long lightEnd = length / 4L;
        long heavyEnd = length * 55L / 100L;
        long blizzardEnd = length * 85L / 100L;

        StormStage stage;
        long stageStart;
        long stageEnd;
        float from;
        float to;

        if (tickInCycle < lightEnd) {
            stage = StormStage.LIGHT;
            stageStart = 0L;
            stageEnd = lightEnd;
            from = 0.25f;
            to = 0.50f;
        } else if (tickInCycle < heavyEnd) {
            stage = StormStage.HEAVY;
            stageStart = lightEnd;
            stageEnd = heavyEnd;
            from = 0.50f;
            to = 0.85f;
        } else if (tickInCycle < blizzardEnd) {
            stage = StormStage.BLIZZARD;
            stageStart = heavyEnd;
            stageEnd = blizzardEnd;
            from = 0.85f;
            to = 1.00f;
        } else {
            stage = StormStage.TAPER;
            stageStart = blizzardEnd;
            stageEnd = length;
            from = 1.00f;
            to = 0.00f;
        }

        long tickInStage = tickInCycle - stageStart;
        long stageLength = stageEnd - stageStart;
        float intensity = lerp(from, to, tickInStage, stageLength);

        return new StormSnapshot(stage, index, dayTimeTicks, tickInStage, stageLength, intensity);
    }

    private static float lerp(float from, float to, long tick, long length) {
        if (length <= 0L) {
            return to;
        }
        float progress = (float) tick / (float) length;
        if (progress < 0.0f) {
            progress = 0.0f;
        } else if (progress > 1.0f) {
            progress = 1.0f;
        }
        return from + (to - from) * progress;
    }

    /** Deterministic, non-negative 31-bit mix of seed and cycle index. */
    private static int hash(long seed, int index) {
        long z = seed + (index & 0xFFFFFFFFL) * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (int) (z & 0x7FFFFFFFL);
    }
}
