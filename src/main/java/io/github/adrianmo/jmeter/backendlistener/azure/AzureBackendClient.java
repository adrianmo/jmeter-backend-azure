package io.github.adrianmo.jmeter.backendlistener.azure;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AzureBackendClient extends AbstractBackendListenerClient {

    private TelemetryClient client;

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        TelemetryClient client = new TelemetryClient(TelemetryConfiguration.createDefault());
        client.getContext().setInstrumentationKey("");
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

        client.trackMetric(name, value, sr.getSampleCount(), value, value, value, properties);
    }

    @Override
    public void handleSampleResults(List<SampleResult> results, BackendListenerContext backendListenerContext) {
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

}
