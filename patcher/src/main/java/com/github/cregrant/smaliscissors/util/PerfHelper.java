package com.github.cregrant.smaliscissors.util;

public class PerfHelper {
    long minTime = Long.MAX_VALUE;
    long lastTime;

    public PerfHelper() {
    }

    public void print(long time) {
        long cycleTime = calc(time);
        if (cycleTime == Long.MAX_VALUE) {
            return;
        }
        System.out.println(cycleTime + " (" + minTime + " min)");
    }

    public void printBest(long time) {
        long lastMinTime = minTime;
        long cycleTime = calc(time);
        if (cycleTime < lastMinTime) {
            System.out.println(cycleTime + " (" + minTime + " min)");
        }
    }

    private long calc(long time) {
        if (lastTime == 0) {
            lastTime = time;
            return Long.MAX_VALUE;
        }
        long cycleTime = time - lastTime;
        lastTime = time;
        minTime = Math.min(cycleTime, minTime);
        return cycleTime;
    }
}
