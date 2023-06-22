package com.github.cregrant.smaliscissors.functional.Utils;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class BackgroundTasks {

    private final ExecutorService worker;
    private final ArrayList<Future<?>> futures = new ArrayList<>(1000);

    public BackgroundTasks(ExecutorService worker) {
        this.worker = worker;
    }

    public void submitTask(Runnable task) {
        futures.add(worker.submit(task));
    }

    public void submitTask(Callable<?> task) {
        futures.add(worker.submit(task));
    }

    public void waitAndClear() {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Computing background task failed:", e);
            }
        }
        futures.clear();
    }

    public ArrayList<Future<?>> waitAndReturn() {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Computing background task failed:", e);
            }
        }
        ArrayList<Future<?>> result = new ArrayList<>(futures);
        futures.clear();
        return result;
    }
}
