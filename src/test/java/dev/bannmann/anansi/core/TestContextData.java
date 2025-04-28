package dev.bannmann.anansi.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.kohsuke.MetaInfServices;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import dev.bannmann.anansi.api.ContextDataProvider;
import dev.bannmann.anansi.api.ContextSupport;

public class TestContextData
{
    static class BackendProblemException extends RuntimeException implements ContextSupport
    {
        private static final String KEY = "responseHeaders";
        private static final String VALUE = "Foo: 42\nConnection: keep-alive";

        @Override
        public Map<String, Object> getContextData()
        {
            return Map.of(KEY, VALUE);
        }
    }

    static class WrappingException extends RuntimeException implements ContextSupport
    {
        private static final String KEY = "wrapContents";
        private static final String VALUE = "kebab";

        public WrappingException(BackendProblemException cause)
        {
            super(cause);
        }

        @Override
        public Map<String, Object> getContextData()
        {
            return Map.of(KEY, VALUE);
        }
    }

    @MetaInfServices
    public static class FakeContextDataProvider extends ContextDataProvider<BackendProblemException>
    {
        private static final String KEY = "fakeRequestTracingId";
        private static final String VALUE = "0xCAFE";

        @Override
        public Class<BackendProblemException> getThrowableClass()
        {
            return BackendProblemException.class;
        }

        @Override
        protected Map<String, Object> getContextData(BackendProblemException throwable)
        {
            return Map.of(KEY, VALUE);
        }
    }

    private Anansi anansi;

    @BeforeClass
    private void setUp()
    {
        anansi = new Anansi("12345", List.of(getClass().getPackageName()), new NoOpIncidentStore());
    }

    @Test
    public void testEmptyContext()
    {
        Incident incident = recordIncident(new ArithmeticException("Really cannot make 5 even"));
        assertThat(incident.getContextData()).isEmpty();
    }

    private Incident recordIncident(Throwable e)
    {
        return anansi.recordIncident(e, Severity.INTERNAL_FAILURE)
            .orElseThrow(AssertionError::new);
    }

    @Test
    public void testContextSupport()
    {
        Incident incident = recordIncident(new BackendProblemException());

        assertContainsBackendProblemContextData(incident);
    }

    private void assertContainsBackendProblemContextData(Incident incident)
    {
        assertThat(incident.getContextData()).containsEntry(BackendProblemException.KEY, BackendProblemException.VALUE);
    }

    @Test
    public void testContextWrapping()
    {
        BackendProblemException cause = new BackendProblemException();
        WrappingException wrappingException = new WrappingException(cause);

        Incident incident = recordIncident(wrappingException);

        assertContainsBackendProblemContextData(incident);
    }

    @Test
    public void testContextDataProvider()
    {
        Incident incident = recordIncident(new BackendProblemException());

        assertThat(incident.getContextData()).containsEntry(FakeContextDataProvider.KEY, FakeContextDataProvider.VALUE);
    }

    @Test
    public void testAttachContextData()
    {
        BackendProblemException cause = new BackendProblemException();
        Anansi.attachingTo(cause)
            .set("subsystemState", "REGULAR")
            .set("subsystemPerformance", 99);

        WrappingException wrappingException = new WrappingException(cause);
        Anansi.attachingTo(wrappingException)
            .set("globalState", "This is fine.");

        Incident incident = recordIncident(wrappingException);
        SoftAssertions.assertSoftly(softly -> softly.assertThat(incident.getContextData())
            .containsEntry("subsystemState", "REGULAR")
            .containsEntry("subsystemPerformance", 99)
            .containsEntry("globalState", "This is fine."));
    }
}
