package com.acantilado.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RetryableBatchedExecutorTest {
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @Test
    void executeUntilAllSuccessful_allSucceed_returnsAllResults() {
        // Given: 10 requests that all succeed
        Set<String> requests = Set.of("req1", "req2", "req3", "req4", "req5",
                "req6", "req7", "req8", "req9", "req10");

        Function<String, String> alwaysSucceed = req -> "result-" + req;

        // When
        Set<String> results = RetryableBatchedExecutor.executeUntilAllSuccessful(
                requests, 5, 10, executorService, alwaysSucceed
        );

        // Then
        assertEquals(10, results.size());
        assertTrue(results.contains("result-req1"));
        assertTrue(results.contains("result-req10"));
    }

    @Test
    void executeUntilAllSuccessful_someFailThenSucceed_retriesAndReturnsAll() {
        // Given: Requests that fail first 2 attempts, then succeed
        AtomicInteger attemptCount = new AtomicInteger(0);
        Set<String> requests = Set.of("req1", "req2", "req3");

        Function<String, String> failTwiceThenSucceed = req -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt <= 6) { // First 2 batches of 3 requests = 6 attempts fail
                return null;
            }
            return "result-" + req;
        };

        // When
        Set<String> results = RetryableBatchedExecutor.executeUntilAllSuccessful(
                requests, 3, 10, executorService, failTwiceThenSucceed
        );

        // Then: All should eventually succeed
        assertEquals(3, results.size());
        assertTrue(results.contains("result-req1"));
        assertTrue(results.contains("result-req2"));
        assertTrue(results.contains("result-req3"));
    }

    @Test
    void executeUntilAllSuccessful_exceedsRetryCount_returnsPartialSuccesses() {
        // Given: 10 requests, 9 succeed, 1 always fails
        Set<String> requests = Set.of("req1", "req2", "req3", "req4", "req5",
                "req6", "req7", "req8", "req9", "stubborn");

        Function<String, String> nineSucceedOneAlwaysFails = req -> {
            if ("stubborn".equals(req)) {
                return null; // Always fails
            }
            return "result-" + req;
        };

        // When: Retry count is 3
        Set<String> results = RetryableBatchedExecutor.executeUntilAllSuccessful(
                requests, 5, 3, executorService, nineSucceedOneAlwaysFails
        );

        // Then: Should return the 9 successful results, NOT an empty set
        assertEquals(9, results.size(),
                "Should return partial successes when retry count exceeded");

        assertTrue(results.contains("result-req1"));
        assertTrue(results.contains("result-req9"));
        assertFalse(results.contains("result-stubborn"));
    }

    @Test
    void executeUntilAllSuccessful_multipleFailuresThenPartialSuccess_returnsAllSuccesses() {
        // Given: Complex scenario - some fail multiple times before succeeding
        AtomicInteger req1Attempts = new AtomicInteger(0);
        AtomicInteger req2Attempts = new AtomicInteger(0);

        Set<String> requests = Set.of("req1", "req2", "req3", "persistent-failure");

        Function<String, String> complexBehavior = req -> {
            switch (req) {
                case "req1":
                    // Fails first 2 attempts
                    return req1Attempts.incrementAndGet() > 2 ? "result-req1" : null;
                case "req2":
                    // Fails first attempt
                    return req2Attempts.incrementAndGet() > 1 ? "result-req2" : null;
                case "req3":
                    // Always succeeds
                    return "result-req3";
                case "persistent-failure":
                    // Always fails
                    return null;
                default:
                    throw new IllegalArgumentException("Unexpected request: " + req);
            }
        };

        // When: Limited retries
        Set<String> results = RetryableBatchedExecutor.executeUntilAllSuccessful(
                requests, 2, 5, executorService, complexBehavior
        );

        // Then: Should get 3 successes (req1, req2, req3) but not persistent-failure
        assertEquals(3, results.size(),
                "Should return all eventual successes even if some permanently fail");

        assertTrue(results.contains("result-req1"));
        assertTrue(results.contains("result-req2"));
        assertTrue(results.contains("result-req3"));
        assertFalse(results.contains("result-persistent-failure"));
    }

    @Test
    void executeUntilAllSuccessful_exceptionThrown_retriesFailedRequest() {
        // Given: Request that throws exception first time, succeeds second time
        AtomicInteger attemptCount = new AtomicInteger(0);
        Set<String> requests = Set.of("exception-thrower");

        Function<String, String> throwThenSucceed = req -> {
            if (attemptCount.incrementAndGet() == 1) {
                throw new RuntimeException("Simulated failure");
            }
            return "result-" + req;
        };

        // When
        Set<String> results = RetryableBatchedExecutor.executeUntilAllSuccessful(
                requests, 1, 5, executorService, throwThenSucceed
        );

        // Then
        assertEquals(1, results.size());
        assertTrue(results.contains("result-exception-thrower"));
    }

    @Test
    void executeUntilAllSuccessful_emptyInput_returnsEmptySet() {
        // Given: Empty request set
        Set<String> requests = Set.of();

        // When
        Set<String> results = RetryableBatchedExecutor.executeUntilAllSuccessful(
                requests, 5, 10, executorService, req -> "result"
        );

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    void executeUntilAllSuccessful_singleRequest_works() {
        // Given: Single request
        Set<String> requests = Set.of("single");

        // When
        Set<String> results = RetryableBatchedExecutor.executeUntilAllSuccessful(
                requests, 5, 10, executorService, req -> "result-" + req
        );

        // Then
        assertEquals(1, results.size());
        assertTrue(results.contains("result-single"));
    }

    @Test
    void executeUntilAllSuccessful_batchSizeSmallerThanTotal_processesInBatches() {
        // Given: 10 requests with batch size of 3
        Set<String> requests = Set.of("req1", "req2", "req3", "req4", "req5",
                "req6", "req7", "req8", "req9", "req10");

        // When
        Set<String> results = RetryableBatchedExecutor.executeUntilAllSuccessful(
                requests, 3, 10, executorService, req -> "result-" + req
        );

        // Then: Should still get all results despite batching
        assertEquals(10, results.size());
    }

    @Test
    void executeUntilAllSuccessful_allRequestsFailPermanently_returnsEmptySet() {
        // Given: All requests always fail
        Set<String> requests = Set.of("fail1", "fail2", "fail3");

        Function<String, String> alwaysFail = req -> null;

        // When: Limited retries
        Set<String> results = RetryableBatchedExecutor.executeUntilAllSuccessful(
                requests, 2, 3, executorService, alwaysFail
        );

        // Then: Should return empty set when everything fails
        assertTrue(results.isEmpty(),
                "Should return empty set when all requests permanently fail");
    }

//    @Test
//    void executeUntilAllSuccessful_exactlyAtRetryLimit_returnsPartialSuccesses() {
//        // Given: Setup that will hit exactly the retry limit
//        AtomicInteger batchCount = new AtomicInteger(0);
//        Set<String> requests = Set.of("req1", "req2");
//
//        Function<String, String> succeedOnlyOnFirstBatch = req -> {
//            int batch = batchCount.incrementAndGet();
//            // req1 succeeds on first batch, req2 never succeeds
//            if ("req1".equals(req) && batch <= 1) {
//                return "result-req1";
//            }
//            return null;
//        };
//
//        // When: Set retry count to exactly when req2 is still failing
//        Set<String> results = RetryableBatchedExecutor.executeUntilAllSuccessful(
//                requests, 2, 2, executorService, succeedOnlyOnFirstBatch
//        );
//
//        // Then: Should get req1's success even though we hit retry limit
//        assertEquals(1, results.size(),
//                "Should return partial success when hitting retry limit exactly");
//        assertTrue(results.contains("result-req1"));
//    }
}