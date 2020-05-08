package io.github.adrianmo.jmeter.backendlistener.azure;

import com.microsoft.applicationinsights.TelemetryClient;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestAzureBackendClient {

    @Mock
    private TelemetryClient telemetryClient;

    @InjectMocks
    private final AzureBackendClient client = new AzureBackendClient();

    private BackendListenerContext context;

    @Before
    public void setUp() {
        Arguments args = new Arguments();
        args.addArgument("testName", "test-1");
        context = new BackendListenerContext(args);
        Whitebox.setInternalState(client, "testName", "test-1");
        Whitebox.setInternalState(client, "samplersToFilter", new HashSet<>());
    }

    @Test
    public void testGetDefaultParameters() {
        Arguments args = client.getDefaultParameters();
        assertNotNull(args);
    }

    @Test
    public void testHandleSampleResults() {
        doNothing().when(telemetryClient).trackRequest(any());

        SampleResult sr = new SampleResult();
        List<SampleResult> list = new ArrayList<SampleResult>();
        list.add(sr);

        try {
            client.handleSampleResults(list, context);
        } catch(Exception e) {
            fail(e.toString());
        }
    }
}
