package io.github.adrianmo.jmeter.backendlistener.azure;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
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
    private static final String INSTRUMENTATION_KEY = "instrumentationKey";

    public AzureBackendClient() {
        super();
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument(INSTRUMENTATION_KEY, "");
        return arguments;
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        client = new TelemetryClient(TelemetryConfiguration.createDefault());
        client.getContext().setInstrumentationKey(context.getParameter(INSTRUMENTATION_KEY));
        super.setupTest(context);
    }

    private void trackMetric(String name, Double value, SampleResult sr) {
        Map<String, String> properties = new HashMap<String, String>();
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

        MetricTelemetry metric = new MetricTelemetry(name, value);
        metric.setCount(sr.getSampleCount());
        metric.setTimestamp(new Date(sr.getTimeStamp()));
        MapUtil.copy(properties, metric.getProperties());
        client.trackMetric(metric);
    }

    @Override
    public void handleSampleResults(List<SampleResult> results, BackendListenerContext context) {
        for (SampleResult sr : results) {
            trackMetric("Bytes", (double)sr.getBytesAsLong(), sr);
            trackMetric("SentBytes", (double)sr.getSentBytes(), sr);
            trackMetric("ConnectTime", (double)sr.getConnectTime(), sr);
            trackMetric("ErrorCount", (double)sr.getErrorCount(), sr);
            trackMetric("IdleTime", (double)sr.getIdleTime(), sr);
            trackMetric("Latency", (double)sr.getLatency(), sr);
            trackMetric("ResponseTime", (double)sr.getTime(), sr);
        }
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        client.flush();
        super.teardownTest(context);
    }

}
