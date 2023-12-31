package dev.bannmann.anansi.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.assertj.core.api.SoftAssertions;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Slf4j
public class TestNestedContexts
{
    private Anansi anansi;
    private IncidentStore mockedIncidentStore;

    @BeforeMethod
    private void setUp()
    {
        mockedIncidentStore = Mockito.mock(IncidentStore.class);

        anansi = new Anansi("12345", List.of(getClass().getPackageName()), mockedIncidentStore);
    }

    @Test
    public void testAccessingDataOfFailedContexts()
    {
        var example = new ExampleForNesting();

        try
        {
            example.run();
        }
        catch (RuntimeException e)
        {
            Incident incident = anansi.recordIncident(e, Severity.INTERNAL_FAILURE)
                .orElseThrow(AssertionError::new);

            SoftAssertions.assertSoftly(softly -> softly.assertThat(incident.getContextData())
                .containsKeys("callingA", "callingB", "callingC")
                .doesNotContainKey("callingD"));
        }
    }

    @Test
    @SuppressWarnings("java:S5778")
    public void testRecordedAndWrappedIncidentsIncludeDataFromOuterMethods()
    {
        var example = new ExampleForWrapping(anansi);

        assertThatThrownBy(example::run).isExactlyInstanceOf(CustomException.class);

        var incident = obtainRecordedIncident();
        assertThat(incident.getContextData()).containsKey("outerMethodData");
    }

    private Incident obtainRecordedIncident()
    {
        var argumentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(mockedIncidentStore).store(argumentCaptor.capture(), ArgumentMatchers.any(StorableFingerprint.class));

        return argumentCaptor.getValue();
    }

    @Test
    public void testRecordedIncidentsIncludeDataFromOuterMethods()
    {
        var example = new ExampleForIgnoring(anansi);
        example.run();

        var incident = obtainRecordedIncident();
        assertThat(incident.getContextData()).containsKey("outerMethodData");
    }
}
