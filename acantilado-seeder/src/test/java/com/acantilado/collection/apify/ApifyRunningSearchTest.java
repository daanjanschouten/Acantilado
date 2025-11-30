package com.acantilado.collection.apify;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApifyRunningSearchTest {

    @Test
    void testThreeArgConstructor_parameterOrderCorrect() {
        // Given
        String testRequest = "test-request";
        String expectedRunId = "A2DkYWiqhGLIeKMqe";
        String expectedDatasetId = "Mf6xawfGoOBNwbeTn";

        // When - Constructor signature: (request, runId, datasetId)
        ApifyRunningSearch<String> search = new ApifyRunningSearch<>(
                testRequest,
                expectedRunId,
                expectedDatasetId
        );

        // Then - Verify fields are assigned correctly (not swapped)
        assertEquals(expectedRunId, search.getRunId(),
                "RunId should match the value passed as 2nd parameter");
        assertEquals(expectedDatasetId, search.getDatasetId(),
                "DatasetId should match the value passed as 3rd parameter");
        assertEquals(testRequest, search.getRequest());
        assertEquals(ApifySearchStatus.TO_BE_SUBMITTED, search.getStatus());
    }

    @Test
    void testThreeArgConstructor_detectsParameterSwap() {
        // This test exists specifically to catch the bug where constructor params were
        // defined as (request, datasetId, runId) but called as (request, runId, datasetId)

        String runId = "RUN-12345";
        String datasetId = "DATASET-67890";

        ApifyRunningSearch<String> search = new ApifyRunningSearch<>(
                "request",
                runId,      // 2nd param should be runId
                datasetId   // 3rd param should be datasetId
        );

        // If parameters are swapped in constructor definition, these assertions will FAIL
        assertNotEquals(datasetId, search.getRunId(),
                "REGRESSION: RunId has dataset value - constructor parameters are swapped!");
        assertNotEquals(runId, search.getDatasetId(),
                "REGRESSION: DatasetId has run value - constructor parameters are swapped!");

        assertEquals(runId, search.getRunId());
        assertEquals(datasetId, search.getDatasetId());
    }

    @Test
    void testFourArgConstructor_allFieldsAssigned() {
        // Given
        String testRequest = "test-request";
        String expectedRunId = "run-123";
        String expectedDatasetId = "dataset-456";
        ApifySearchStatus expectedStatus = ApifySearchStatus.RUNNING;

        // When
        ApifyRunningSearch<String> search = new ApifyRunningSearch<>(
                testRequest,
                expectedStatus,
                expectedRunId,
                expectedDatasetId
        );

        // Then
        assertEquals(testRequest, search.getRequest());
        assertEquals(expectedStatus, search.getStatus());
        assertEquals(expectedRunId, search.getRunId());
        assertEquals(expectedDatasetId, search.getDatasetId());
    }

    @Test
    void testWithStatus_preservesAllFields() {
        // Given
        String request = "original-request";
        String runId = "run-999";
        String datasetId = "dataset-888";
        ApifyRunningSearch<String> original = new ApifyRunningSearch<>(
                request,
                runId,
                datasetId
        );

        // When
        ApifyRunningSearch<String> updated = original.withStatus(ApifySearchStatus.SUCCEEDED);

        // Then
        assertEquals(ApifySearchStatus.SUCCEEDED, updated.getStatus());
        assertEquals(request, updated.getRequest(), "Request should be preserved");
        assertEquals(runId, updated.getRunId(), "RunId should be preserved");
        assertEquals(datasetId, updated.getDatasetId(), "DatasetId should be preserved");
    }

    @Test
    void testWithStatus_doesNotMutateOriginal() {
        // Given
        ApifyRunningSearch<String> original = new ApifyRunningSearch<>(
                "request",
                "run-id",
                "dataset-id"
        );
        ApifySearchStatus originalStatus = original.getStatus();

        // When
        ApifyRunningSearch<String> updated = original.withStatus(ApifySearchStatus.FAILED);

        // Then
        assertEquals(originalStatus, original.getStatus(), "Original should be immutable");
        assertEquals(ApifySearchStatus.FAILED, updated.getStatus());
    }

    @Test
    void testRealWorldScenario_apifyResponseMapping() {
        // This simulates the actual bug scenario from production

        // Simulating the Apify API response values
        String actualRunIdFromApi = "A2DkYWiqhGLIeKMqe";           // From "id" field
        String actualDatasetIdFromApi = "Mf6xawfGoOBNwbeTn";      // From "defaultDatasetId" field

        // This is how the code calls the constructor in triggerSearches()
        ApifyRunningSearch<String> search = new ApifyRunningSearch<>(
                "test-request",
                actualRunIdFromApi,      // Passing runId as 2nd param
                actualDatasetIdFromApi   // Passing datasetId as 3rd param
        );

        // When checking status, we use getRunId() for the API call
        String runIdForStatusCheck = search.getRunId();

        // This MUST be the actual run ID, not the dataset ID
        assertEquals(actualRunIdFromApi, runIdForStatusCheck,
                "CRITICAL: getRunId() must return the run ID, not dataset ID. " +
                        "This causes 'Actor run was not found' errors in production!");

        // When fetching results, we use getDatasetId()
        String datasetIdForResultsFetch = search.getDatasetId();

        assertEquals(actualDatasetIdFromApi, datasetIdForResultsFetch,
                "CRITICAL: getDatasetId() must return the dataset ID, not run ID.");
    }
}