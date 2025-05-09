package dev.bannmann.anansi.core;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.IOException;
import java.net.HttpRetryException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import lombok.Getter;

import org.assertj.core.util.Throwables;
import org.kohsuke.MetaInfServices;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.github.mizool.core.exception.CodeInconsistencyException;
import com.github.mizool.core.exception.StoreLayerException;
import dev.bannmann.anansi.api.Fingerprintable;
import dev.bannmann.anansi.api.Fingerprinter;
import dev.bannmann.anansi.api.FrameData;
import dev.bannmann.anansi.api.StackFrame;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;

public class TestFingerprinting
{
    static class BackendProblemException extends RuntimeException implements Fingerprintable
    {
        @Getter
        private final int status;

        public BackendProblemException(String message, int status)
        {
            super(message);
            this.status = status;
        }

        @Override
        public Map<String, Object> getFingerprintData()
        {
            return Map.of("status", status);
        }
    }

    @MetaInfServices
    public static class HttpRetryExceptionFingerprinter extends Fingerprinter<HttpRetryException>
    {
        @Override
        public Class<HttpRetryException> getThrowableClass()
        {
            return HttpRetryException.class;
        }

        @Override
        protected Map<String, Object> extractData(HttpRetryException throwable)
        {
            return Map.of("responseCode", throwable.responseCode());
        }
    }

    static class SimulatedException extends RuntimeException
    {
    }

    private Anansi anansi;

    @BeforeClass
    private void setUp()
    {
        anansi = new Anansi("12345", List.of(getClass().getPackageName()), new NoOpIncidentStore());
    }

    /**
     * Tests that any frames associated with the IDE, TestNG and reflection are filtered out.
     */
    @Test
    public void testBasics()
    {
        FingerprintData
            data
            = recordIncident(new ArithmeticException("Really cannot make 5 even")).getFingerprintData();

        assertThat(data.getThrowableClassName()).isEqualTo("java.lang.ArithmeticException");

        assertThat(data.getRelevantFrames()).hasSize(1)
            .map(FrameData::getLocation)
            .containsExactly(getClass().getName() + ".testBasics");
    }

    private Incident recordIncident(Throwable e)
    {
        return anansi.recordIncident(e, Severity.INTERNAL_FAILURE)
            .orElseThrow(AssertionError::new);
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    @SuppressWarningsRationale("We intentionally pass null to the URL constructor to cause an exception")
    public void testLocationAndFrames()
    {
        try
        {
            new URL(null);
        }
        catch (MalformedURLException e)
        {
            FingerprintData data = recordIncident(e).getFingerprintData();

            assertSoftly(softly -> {
                // The fingerprint data includes all wrapping exceptions, but throwableClassName is the root cause
                softly.assertThat(data.getThrowableClassName())
                    .isEqualTo("java.lang.NullPointerException");

                // The location always refers to an app class, even if the 'throw' statement is not inside the app.
                softly.assertThat(data.getLocation())
                    .isNotNull()
                    .extracting(FrameData::getClassName, as(STRING))
                    .isEqualTo(getClass().getName());

                // The stack trace actually contains 3 frames within URL class, but Anansi only includes the topmost.
                softly.assertThat(data.getRelevantFrames())
                    .hasSize(2)
                    .map(FrameData::getLocation)
                    .containsExactly("java.net.URL.<init>", getClass().getName() + ".testLocationAndFrames");
            });
        }
    }

    @Test
    public void testAnnotatedLocation() throws IOException
    {
        try
        {
            silentlyCreateUrl();
        }
        catch (MalformedURLException e)
        {
            FingerprintData data = recordIncident(e).getFingerprintData();

            assertSoftly(softly -> {
                // The topmost application method has incidentLocation=false, so the location is its caller.
                softly.assertThat(data.getLocation())
                    .isNotNull()
                    .extracting(FrameData::getMethodName, as(STRING))
                    .isEqualTo("testAnnotatedLocation");

                // Disabling silentlyCreateUrl() as an incident location should not alter its relevant frame list
                softly.assertThat(data.getRelevantFrames())
                    .map(FrameData::getLocation)
                    .containsExactly("java.net.URL.<init>",
                        getClass().getName() + ".silentlyCreateUrl",
                        getClass().getName() + ".testAnnotatedLocation");
            });
        }
    }

    @StackFrame(incidentLocation = false)
    @SuppressWarnings("DataFlowIssue")
    @SuppressWarningsRationale("We intentionally pass null to the URL constructor to cause an exception")
    private void silentlyCreateUrl() throws IOException
    {
        new URL(null);
    }

    @Test
    public void testStacklessException()
    {
        var throwable = new BrokenBarrierException("I'm lost!");
        throwable.setStackTrace(new StackTraceElement[]{});

        var incident = recordIncident(throwable);

        assertThat(incident.getThrowableDetails()
            .stream()
            .map(ThrowableData::toString)).containsExactly(
            "java.util.concurrent.BrokenBarrierException at [?]: I'm lost!");
    }

    @Test
    public void testWrappedThrowables()
    {
        try
        {
            secondWrap();
            fail("No exception thrown");
        }
        catch (CodeInconsistencyException e)
        {
            FingerprintData data = recordIncident(e).getFingerprintData();

            assertThat(data.getRelevantFrames()).map(FrameData::getLocation)
                .containsExactly(getClass().getName() + ".originalMethod",
                    getClass().getName() + ".firstWrap",
                    getClass().getName() + ".secondWrap",
                    getClass().getName() + ".testWrappedThrowables");
        }
    }

    private void secondWrap()
    {
        try
        {
            firstWrap();
        }
        catch (StoreLayerException e)
        {
            throw new CodeInconsistencyException(e);
        }
    }

    private void firstWrap()
    {
        try
        {
            originalMethod();
        }
        catch (SimulatedException e)
        {
            throw new StoreLayerException(e);
        }
    }

    private void originalMethod()
    {
        throw new SimulatedException();
    }

    /**
     * Verifies that when the root cause doesn't have a stack trace, the relevant frame list is still filled. Reuses
     * code from {@link #testWrappedThrowables()}.
     */
    @Test
    public void testWrappedThrowablesWithStacklessRoot()
    {
        try
        {
            secondWrap();
            fail("No exception thrown");
        }
        catch (CodeInconsistencyException e)
        {
            // Let's pretend the root cause exception did not include a stack trace
            Throwables.getRootCause(e)
                .setStackTrace(new StackTraceElement[]{});

            FingerprintData data = recordIncident(e).getFingerprintData();

            assertThat(data.getRelevantFrames()).map(FrameData::getLocation)
                .containsExactly(getClass().getName() + ".firstWrap",
                    getClass().getName() + ".secondWrap",
                    getClass().getName() + ".testWrappedThrowablesWithStacklessRoot");
        }
    }

    @Test
    public void testFramesMultithreading()
    {
        CompletableFuture<Void> future = CompletableFuture.runAsync(this::threadRun);

        try
        {
            waitForCompletion(future);
            fail("Exception was not forwarded to the main thread");
        }
        catch (InterruptedException | ExecutionException e)
        {
            Incident incident = recordIncident(e);

            assertThat(incident.getFingerprintData()
                .getRelevantFrames()).map(FrameData::getLocation)
                .containsExactly(getClass().getName() + ".threadRun",
                    getClass().getName() + ".waitForCompletion",
                    getClass().getName() + ".testFramesMultithreading");
        }
    }

    private void threadRun()
    {
        throw new SimulatedException();
    }

    private void waitForCompletion(CompletableFuture<Void> future) throws InterruptedException, ExecutionException
    {
        future.get();
    }

    @Test
    public void testFingerprintIgnoresMessageDifference()
    {
        List<Incident> incidents = new ArrayList<>();

        for (String message : List.of("Not in Kansas anymore",
            "Spline count below 10%",
            "Personality core 0X94F6 missing"))
        {
            Incident incident = recordIncident(new RuntimeException(message));
            incidents.add(incident);
        }

        assertDistinctFingerprintCount(incidents, 1);
        assertDistinctDataHashCodeCount(incidents, 1);
    }

    private void assertDistinctFingerprintCount(List<Incident> incidents, int expected)
    {
        Set<String> fingerprints = incidents.stream()
            .map(Incident::getFingerprint)
            .collect(Collectors.toSet());

        assertThat(fingerprints).hasSize(expected);
    }

    private void assertDistinctDataHashCodeCount(List<Incident> incidents, int expected)
    {
        Set<Integer> hashCodes = incidents.stream()
            .map(Incident::getFingerprintData)
            .map(FingerprintData::hashCode)
            .collect(Collectors.toSet());

        assertThat(hashCodes).hasSize(expected);
    }

    @Test
    public void testFingerprintableException()
    {
        List<Incident> incidents = new ArrayList<>();

        for (Integer status : List.of(402, 503, 504))
        {
            String method = "GET";
            String url = "https://example.com/fa3eb2b8281f?from=2023-01-21&to=2023-02-09";
            String message = String.format("Got status %d for %s %s", status, method, url);

            Throwable throwable = new BackendProblemException(message, status);

            Incident incident = recordIncident(throwable);
            incidents.add(incident);
        }

        assertDistinctFingerprintCount(incidents, 3);
        assertDistinctDataHashCodeCount(incidents, 3);
    }

    @Test
    public void testFingerprinter()
    {
        List<Incident> incidents = new ArrayList<>();

        var exceptions = List.of(new HttpRetryException("Unavailable due to maintenance", 503),
            new HttpRetryException("Outside office hours", 503),
            new HttpRetryException("Disk full", 507));

        for (HttpRetryException exception : exceptions)
        {
            Incident incident = recordIncident(exception);
            incidents.add(incident);
        }

        assertDistinctFingerprintCount(incidents, 2);
        assertDistinctDataHashCodeCount(incidents, 2);
    }
}
