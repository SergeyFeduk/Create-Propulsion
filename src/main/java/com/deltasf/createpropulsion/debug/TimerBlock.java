package com.deltasf.createpropulsion.debug;

public class TimerBlock implements AutoCloseable {
    private final String blockName;
    private final long startTime;

    public TimerBlock(String blockName) {
        this.blockName = blockName;
        this.startTime = System.nanoTime();
    }

    @Override
    public void close() {
        long endTime = System.nanoTime();
        long durationNanos = endTime - startTime;
        double durationMicros = durationNanos / 1000.0;
        double durationMillis = durationNanos / 1_000_000.0;
        System.out.printf("[Profiler] '%s' took %.3f µs (%.6f ms)%n", this.blockName, durationMicros, durationMillis);
    }
}
