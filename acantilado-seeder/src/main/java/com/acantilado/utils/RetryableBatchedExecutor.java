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
import java.util.stream.Collectors;

public class RetryableBatchedExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryableBatchedExecutor.class);
    private static final int INITIAL_RETRY_DELAY_MS = 10000;

    private record SingleRunStats(int totalToRun, int totalExecuted, int executedThisRun, int totalRetryCount, long waitTimeMs) {
        SingleRunStats refresh(int executedThisRun) {
            return new SingleRunStats(
                    this.totalToRun,
                    this.totalExecuted + executedThisRun,
                    executedThisRun,
                    this.totalRetryCount + 1,
                    adjustWaitTime(this.waitTimeMs, this.executedThisRun(), executedThisRun)
            );
        }

        static long adjustWaitTime(long current, int numPrevResults, int numCurrentResults) {
            return numPrevResults > numCurrentResults || numCurrentResults == 0
                    ? (long) (current * 1.5)
                    : (long) (current * 0.8);
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

            List<CompletableFuture<T>> futures = nextBatch
                    .stream()
                    .map( request -> CompletableFuture.supplyAsync(() -> {
                            T response = resultFunction.apply(request);
                            if (Objects.isNull(response)) { // execution has failed
                                LOGGER.debug("Request {} failed to run and will be retried", request);
                                requestsToRun.add(request);
                                return null;
                            }

                            return response;
                        }, executorService))
                    .toList();

            Set<T> successfulResponses = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .peek(requestsThatSucceeded::add)
                    .collect(Collectors.toSet());

            if (!requestsToRun.isEmpty()) {
                currentRun = currentRun.refresh(successfulResponses.size());
                LOGGER.info("Single run stats {}", currentRun);

                if (currentRun.totalRetryCount >= 20) {
                    LOGGER.error("Giving up on retries, {} remaining requests {}", requestsToRun.size(), requestsToRun);
                    throw new RuntimeException("Giving up on subsequent retries");
                }

                try {
                    Thread.sleep(currentRun.waitTimeMs);
                } catch (InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }
        return requestsThatSucceeded;
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
