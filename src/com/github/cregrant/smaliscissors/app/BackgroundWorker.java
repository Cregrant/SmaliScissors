package com.github.cregrant.smaliscissors.app;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class BackgroundWorker {
    static ExecutorService executor = Executors.newWorkStealingPool();

/*    static Future<?> execute(Runnable r) {
        if (executor.isShutdown())
            executor = Executors.newWorkStealingPool();
        return executor.submit(r);
    }

    static Future<?> execute(Callable<?> c) {
        if (executor.isShutdown())
            executor = Executors.newWorkStealingPool();
        return executor.submit(c);
    }

    static void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(200, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }*/
}
