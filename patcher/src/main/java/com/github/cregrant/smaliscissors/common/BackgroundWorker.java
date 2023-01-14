package com.github.cregrant.smaliscissors.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class BackgroundWorker {

    private static final Logger logger = LoggerFactory.getLogger(BackgroundWorker.class);
    private final int threadsNum = Runtime.getRuntime().availableProcessors();
    private ExecutorService executor;

    public void waitForFinish(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Computing background task failed:", e);
            }
        }
    }

    public void start() {
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    }

    public void stop() {
        executor.shutdownNow();
    }

    public int getThreadsNum() {
        return threadsNum;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }
}
