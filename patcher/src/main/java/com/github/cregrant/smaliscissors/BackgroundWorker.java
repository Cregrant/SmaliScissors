package com.github.cregrant.smaliscissors;


import java.util.ArrayList;
import java.util.concurrent.*;

public class BackgroundWorker {
    private static final int cpuCount = Runtime.getRuntime().availableProcessors() > 1 ? Runtime.getRuntime().availableProcessors() - 1 : 1;
    public static ExecutorService executor = Executors.newFixedThreadPool(cpuCount);

    public static void compute(ArrayList<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
