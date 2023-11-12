package com.github.cregrant.smaliscissors.common;


import com.github.cregrant.smaliscissors.Flags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class BackgroundWorker {

    private static final Logger logger = LoggerFactory.getLogger(BackgroundWorker.class);
    private static final int THREADS_COUNT = Runtime.getRuntime().availableProcessors() + 1;
    private final ExecutorService executor;

    public BackgroundWorker() {
        this(THREADS_COUNT);
    }

    public BackgroundWorker(int threads) {
        if (Flags.DEBUG_NO_MULTITHREADING) {
            executor = Executors.newFixedThreadPool(1);
        } else {
            executor = Executors.newFixedThreadPool(threads);
        }
    }

    public void waitForFinish(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Computing background task failed.");
                throw new RuntimeException(e);
            }
        }
    }

    public void stop() {
        executor.shutdownNow();
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
