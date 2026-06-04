package org.example.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AppExecutors {

    private AppExecutors() {
    }

    private static final ThreadFactory IO_THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "app-io-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    };

    public static final ExecutorService IO =
            Executors.newFixedThreadPool(4, IO_THREAD_FACTORY);

    public static void shutdown() {
        IO.shutdown();
        try {
            if (!IO.awaitTermination(5, TimeUnit.SECONDS)) {
                IO.shutdownNow();
            }
        } catch (InterruptedException e) {
            IO.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}