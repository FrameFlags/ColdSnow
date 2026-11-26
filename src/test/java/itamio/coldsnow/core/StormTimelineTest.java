package itamio.coldsnow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StormTimelineTest {

    private static final StormStage[] STAGES = {
            StormStage.LIGHT, StormStage.HEAVY, StormStage.BLIZZARD, StormStage.TAPER
    };
    private static final float[] INTENSITY_START = {0.25f, 0.50f, 0.85f, 1.00f};

    @Test
    void everyStageFirstAndLastTickIsExact() {
        StormTimeline timeline = new StormTimeline(StormTiming.defaults());
        for (int cycle : new int[]{-2, -1, 0, 1, 2, 7}) {
            assertStageBoundaries(timeline, cycle);
        }
    }

    private static void assertStageBoundaries(StormTimeline timeline, int cycle) {
        long start = timeline.cycleStartTicks(cycle);
        long length = timeline.cycleLengthTicks(cycle);
        long[] bounds = {0L, length / 4L, length * 55L / 100L, length * 85L / 100L, length};

        for (int i = 0; i < STAGES.length; i++) {
            long first = start + bounds[i];
            long last = start + bounds[i + 1] - 1L;
            long stageLength = bounds[i + 1] - bounds[i];

            StormSnapshot firstSnap = timeline.at(first);
            assertEquals(STAGES[i], firstSnap.stage(), "first tick of stage " + STAGES[i]);
            assertEquals(cycle, firstSnap.cycleIndex());
            assertEquals(first, firstSnap.dayTimeTicks());
            assertEquals(0L, firstSnap.tickInStage());
            assertEquals(stageLength, firstSnap.stageLengthTicks());
            assertEquals(INTENSITY_START[i], firstSnap.intensity(), 1e-6f);

            StormSnapshot lastSnap = timeline.at(last);
            assertEquals(STAGES[i], lastSnap.stage(), "last tick of stage " + STAGES[i]);
            assertEquals(cycle, lastSnap.cycleIndex());
            assertEquals(stageLength - 1L, lastSnap.tickInStage());
            assertEquals(stageLength, lastSnap.stageLengthTicks());
        }

        StormSnapshot nextCycle = timeline.at(start + length);
        assertEquals(StormStage.LIGHT, nextCycle.stage());
        assertEquals(cycle + 1, nextCycle.cycleIndex());
        assertEquals(0L, nextCycle.tickInStage());
    }

    @Test
    void atIsDeterministicForPositiveNegativeAndLargeTimes() {
        StormTimeline a = new StormTimeline(StormTiming.defaults());
        StormTimeline b = new StormTimeline(StormTiming.defaults());
        long[] ticks = {
                -480_001L, -336_000L, -42L, -1L, 0L, 1L, 83_999L, 84_000L,
                335_999L, 336_000L, 1_234_567L, 987_654_321L
        };
        for (long tick : ticks) {
            assertEquals(a.at(tick), b.at(tick), "same timing must give same snapshot at " + tick);
            assertEquals(a.at(tick), a.at(tick), "at() must be repeatable at " + tick);
        }
    }

    @Test
    void cycleLengthRespectsMinAndMaxDays() {
        StormTimeline timeline = new StormTimeline(StormTiming.defaults());
        for (int i = -20; i <= 100; i++) {
            long length = timeline.cycleLengthTicks(i);
            assertTrue(length >= 14L * StormTimeline.DAY_TICKS, "cycle " + i);
            assertTrue(length <= 20L * StormTimeline.DAY_TICKS, "cycle " + i);
        }

        StormTimeline minOnly = new StormTimeline(new StormTiming(14, 14, 99L));
        StormTimeline maxOnly = new StormTimeline(new StormTiming(20, 20, 99L));
        for (int i = -5; i <= 20; i++) {
            assertEquals(14L * StormTimeline.DAY_TICKS, minOnly.cycleLengthTicks(i));
            assertEquals(20L * StormTimeline.DAY_TICKS, maxOnly.cycleLengthTicks(i));
        }
        assertEquals(14, new StormTiming(20, 14, 1L).minDays(),
                "constructor clamps minDays down to maxDays");
    }

    @Test
    void backwardTimeSetIsHandled() {
        StormTimeline timeline = new StormTimeline(StormTiming.defaults());
        long previousLength = timeline.cycleLengthTicks(-1);

        StormSnapshot atPreviousStart = timeline.at(-previousLength);
        assertEquals(-1, atPreviousStart.cycleIndex());
        assertEquals(StormStage.LIGHT, atPreviousStart.stage());
        assertEquals(0L, atPreviousStart.tickInStage());

        StormSnapshot justBeforeZero = timeline.at(-1L);
        assertEquals(-1, justBeforeZero.cycleIndex());
        assertEquals(StormStage.TAPER, justBeforeZero.stage());
        assertEquals(previousLength - 1L - previousLength * 85L / 100L,
                justBeforeZero.tickInStage());

        StormSnapshot zero = timeline.at(0L);
        assertEquals(0, zero.cycleIndex());
        assertEquals(StormStage.LIGHT, zero.stage());
        assertEquals(0L, zero.tickInStage());
    }

    @Test
    void cycleIndexNeverJumpsMoreThanOneBetweenAdjacentTicks() {
        StormTimeline timeline = new StormTimeline(StormTiming.defaults());
        long previous = timeline.at(-100_000L).cycleIndex();
        for (long tick = -99_999L; tick <= 1_000_000L; tick += 997L) {
            int current = timeline.at(tick).cycleIndex();
            assertTrue(current == previous || current == previous + 1,
                    "unexpected cycle jump at tick " + tick + ": " + previous + " -> " + current);
            previous = current;
        }
    }
}
