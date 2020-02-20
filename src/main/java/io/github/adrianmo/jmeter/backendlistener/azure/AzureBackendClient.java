package io.github.adrianmo.jmeter.backendlistener.azure;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulse;
import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AzureBackendClient extends AbstractBackendListenerClient {

    private TelemetryClient client;
    private static final String TEST_NAME = "testName";
    private static final String INSTRUMENTATION_KEY = "instrumentationKey";
    private static final String LIVE_METRICS = "liveMetrics";

    public AzureBackendClient() {
        super();
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument(TEST_NAME, "jmeter");
        arguments.addArgument(INSTRUMENTATION_KEY, "");
        arguments.addArgument(LIVE_METRICS, "true");

        return arguments;
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        boolean liveMetrics = context.getBooleanParameter(LIVE_METRICS, true);
        TelemetryConfiguration config = TelemetryConfiguration.createDefault();
        config.setInstrumentationKey(context.getParameter(INSTRUMENTATION_KEY));
        client = new TelemetryClient(config);
        if (liveMetrics) {
            QuickPulse.INSTANCE.initialize(config);
        }
        super.setupTest(context);
    }

    private void trackRequest(String name, SampleResult sr) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("Bytes", Long.toString(sr.getBytesAsLong()));
        properties.put("SentBytes", Long.toString(sr.getSentBytes()));
        properties.put("ConnectTime", Long.toString(sr.getConnectTime()));
        properties.put("ErrorCount", Integer.toString(sr.getErrorCount()));
        properties.put("IdleTime", Double.toString(sr.getIdleTime()));
        properties.put("Latency", Double.toString(sr.getLatency()));
        properties.put("BodySize", Long.toString(sr.getBodySizeAsLong()));
        properties.put("TestStartTime", Long.toString(JMeterContextService.getTestStartTime()));
        properties.put("SampleStartTime", Long.toString(sr.getStartTime()));
        properties.put("SampleEndTime", Long.toString(sr.getEndTime()));
        properties.put("SampleLabel", sr.getSampleLabel());
        properties.put("ThreadName", sr.getThreadName());
        properties.put("URL", sr.getUrlAsString());
        properties.put("ResponseCode", sr.getResponseCode());
        properties.put("GrpThreads", Integer.toString(sr.getGroupThreads()));
        properties.put("AllThreads", Integer.toString(sr.getAllThreads()));
        properties.put("SampleCount", Integer.toString(sr.getSampleCount()));

        Date timestamp = new Date(sr.getTimeStamp());
        Duration duration = new Duration(sr.getTime());
        RequestTelemetry req = new RequestTelemetry(name, timestamp, duration, sr.getResponseCode(), sr.getErrorCount() == 0);
        req.setUrl(sr.getURL());
        MapUtil.copy(properties, req.getProperties());
        client.trackRequest(req);
    }

    @Override
    public void handleSampleResults(List<SampleResult> results, BackendListenerContext context) {
        for (SampleResult sr : results) {
            trackRequest(context.getParameter(TEST_NAME), sr);
        }
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        client.flush();
        super.teardownTest(context);
    }
}
