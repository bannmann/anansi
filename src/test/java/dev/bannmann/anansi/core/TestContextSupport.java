package dev.bannmann.anansi.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import dev.bannmann.anansi.api.ContextSupport;

@Slf4j
public class TestContextSupport
{
    @StandardException
    static class ExceptionA extends RuntimeException implements ContextSupport
    {
        @Override
        public Map<String, Object> getContextData()
        {
            return Map.of("foo", "a");
        }
    }

    @StandardException
    static class ExceptionB extends RuntimeException implements ContextSupport
    {
        @Override
        public Map<String, Object> getContextData()
        {
            return Map.of("foo", "b");
        }
    }

    private Anansi anansi;

    @BeforeMethod
    private void setUp()
    {
        IncidentStore mockedIncidentStore = Mockito.mock(IncidentStore.class);
        anansi = new Anansi("12345", List.of(getClass().getPackageName()), mockedIncidentStore);
    }

    @Test
    public void testOutermostExceptionContextDataWins()
    {
        var exceptionA = new ExceptionA();
        var exceptionB = new ExceptionB(exceptionA);

        Incident incident = anansi.recordIncident(exceptionB, Severity.INTERNAL_FAILURE)
            .orElseThrow(AssertionError::new);

        assertThat(incident.getContextData()).containsEntry("foo", "b");
    }
}
