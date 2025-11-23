package com.acantilado.utils;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public class RetryableBatchedExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryableBatchedExecutor.class);
    private static final int INITIAL_RETRY_DELAY_MS = 10000;
    private static final int MAX_RETRY_DELAY_MS = 100000;

    private record SingleRunStats(int totalToRun, int totalExecuted, int executedThisRun, int totalRetryCount, long waitTimeMs) {
        SingleRunStats refreshRetryCount(int executedThisRun) {
            return new SingleRunStats(
                    this.totalToRun,
                    this.totalExecuted + executedThisRun,
                    executedThisRun,
                    this.totalRetryCount + 1,
                    adjustWaitTime(this.waitTimeMs, this.executedThisRun(), executedThisRun)
            );
        }

        static long adjustWaitTime(long current, int numPrevResults, int numCurrentResults) {
            long waitTime = numPrevResults > numCurrentResults || numCurrentResults == 0
                    ? (long) (current * 1.5)
                    : (long) (current * 0.8);

            return waitTime > MAX_RETRY_DELAY_MS
                    ? MAX_RETRY_DELAY_MS
                    : waitTime;
        }

        @Override
        public String toString() {
            return "SingleRunStats{" +
                    "totalToRun=" + totalToRun +
                    ", totalExecuted=" + totalExecuted +
                    ", executedThisRun=" + executedThisRun +
                    ", totalRetryCount=" + totalRetryCount +
                    ", waitTimeMs=" + waitTimeMs +
                    '}';
        }
    }

    private static class BatchResult<S, T> {
        final Set<T> successful;
        final Set<S> failed;

        BatchResult(Set<T> successful, Set<S> failed) {
            this.successful = successful;
            this.failed = failed;
        }
    }

    public static <S, T> Set<T> executeUntilAllSuccessful(
            Set<S> toRun, Function<S, T> resultFunction, Optional<Integer> maybeBatchSize, ExecutorService executorService) {
        int batchSize = maybeBatchSize.orElse(toRun.size());
        Queue<S> requestsToRun = new ConcurrentLinkedQueue<>(toRun);
        Set<T> requestsThatSucceeded = ConcurrentHashMap.newKeySet();

        SingleRunStats currentRun = new SingleRunStats(toRun.size(), 0, 0, 0, INITIAL_RETRY_DELAY_MS);
        while (!requestsToRun.isEmpty()) {
            Set<S> nextBatch = new HashSet<>();
            for (int i = 0; i < batchSize && !requestsToRun.isEmpty(); i++) {
                nextBatch.add(requestsToRun.poll());
            }

            // Process batch and explicitly track successes and failures
            BatchResult<S, T> batchResult = processBatch(nextBatch, resultFunction, executorService);

            // Add successful responses to the overall success set
            requestsThatSucceeded.addAll(batchResult.successful);

            // CRITICAL FIX: Re-add failed requests AFTER all futures have completed
            // This eliminates the race condition
            requestsToRun.addAll(batchResult.failed);

            // Now check if there are still items to process
            if (!requestsToRun.isEmpty()) {
                currentRun = currentRun.refreshRetryCount(batchResult.successful.size());
                LOGGER.info("Single run stats {}", currentRun);

                if (currentRun.totalRetryCount >= 30) {
                    LOGGER.error("Giving up on retries, {} remaining requests {}", requestsToRun.size(), requestsToRun);
                    return Set.of();
                }

                try {
                    Thread.sleep(currentRun.waitTimeMs);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(exception);
                }
            }
        }
        return requestsThatSucceeded;
    }

    private static <S, T> BatchResult<S, T> processBatch(
            Set<S> batch, Function<S, T> resultFunction, ExecutorService executorService) {

        Set<T> successful = ConcurrentHashMap.newKeySet();
        Set<S> failed = ConcurrentHashMap.newKeySet();

        List<CompletableFuture<Void>> futures = batch.stream()
                .map(request -> CompletableFuture.runAsync(() -> {
                    try {
                        T response = resultFunction.apply(request);
                        if (Objects.isNull(response)) {
                            LOGGER.debug("Request {} failed to run and will be retried", request);
                            failed.add(request);
                        } else {
                            successful.add(response);
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Request {} threw exception and will be retried", request, e);
                        failed.add(request);
                    }
                }, executorService))
                .toList();

        // Wait for ALL futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return new BatchResult<>(successful, failed);
    }

    public static <T> T executeCallableInSessionWithoutTransaction(SessionFactory sessionFactory, Callable<T> callable) {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);

        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    public static <T> T executeCallableInSessionWithTransaction(SessionFactory sessionFactory, Callable<T> callable) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        ManagedSessionContext.bind(session);

        try {
            T result = callable.call();
            transaction.commit();
            return result;
        } catch (Exception e) {
            transaction.rollback();
            throw new RuntimeException(e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }

    public static void executeRunnableInSessionWithTransaction(SessionFactory sessionFactory, Runnable runnable) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        ManagedSessionContext.bind(session);

        try {
            runnable.run();
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new RuntimeException(e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }
}