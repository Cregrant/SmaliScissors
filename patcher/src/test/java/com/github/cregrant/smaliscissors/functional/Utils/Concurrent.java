package com.github.cregrant.smaliscissors.functional.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Concurrent {

    //calc something asap
    public static final ExecutorService WORKER = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    //start a long-running background task
    public static final ExecutorService LONG_WORKER = Executors.newCachedThreadPool();
    public static final Semaphore compressSemaphore = new Semaphore(getThreadsByMemory(800));
    public static final Semaphore runPatcherSemaphore = new Semaphore(getThreadsByMemory(800));

    public static int getThreadsByMemory(long megabytesPerThread) {
        return Math.max(1, (int) (Runtime.getRuntime().maxMemory() / megabytesPerThread * 1024 * 1024));
    }
}
