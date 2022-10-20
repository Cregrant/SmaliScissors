package com.github.cregrant.smaliscissors.common;


import java.util.List;
import java.util.concurrent.*;

public class BackgroundWorker {
    private ExecutorService executor;

    public void waitForFinish(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
    }

    public void stop() {
        executor.shutdownNow();
    }

    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }
}
