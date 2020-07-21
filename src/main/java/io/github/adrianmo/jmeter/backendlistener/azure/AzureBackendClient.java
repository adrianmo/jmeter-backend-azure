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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AzureBackendClient extends AbstractBackendListenerClient {

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(AzureBackendClient.class);

    /**
     * Argument keys.
     */
    private static final String KEY_TEST_NAME = "testName";
    private static final String KEY_INSTRUMENTATION_KEY = "instrumentationKey";
    private static final String KEY_LIVE_METRICS = "liveMetrics";
    private static final String KEY_SAMPLERS_LIST = "samplersList";
    private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "useRegexForSamplerList";

    /**
     * Default argument values.
     */
    private static final String DEFAULT_TEST_NAME = "jmeter";
    private static final String DEFAULT_INSTRUMENTATION_KEY = "";
    private static final boolean DEFAULT_LIVE_METRICS = true;
    private static final String DEFAULT_SAMPLERS_LIST = "";
    private static final boolean DEFAULT_USE_REGEX_FOR_SAMPLER_LIST = false;

    /**
     * Separator for samplers list.
     */
    private static final String SEPARATOR = ";";

    /**
     * Application Insights telemetry client.
     */
    private TelemetryClient telemetryClient;

    /**
     * Name of the test.
     */
    private String testName;

    /**
     * Whether to send metrics to the Live Metrics Stream.
     */
    private boolean liveMetrics;

    /**
     * List of samplers to record.
     */
    private String samplersList = "";

    /**
     * Regex if samplers are defined through regular expression.
     */
    private Boolean useRegexForSamplerList;

    /**
     * Set of samplers to record.
     */
    private Set<String> samplersToFilter;

    public AzureBackendClient() {
        super();
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument(KEY_TEST_NAME, DEFAULT_TEST_NAME);
        arguments.addArgument(KEY_INSTRUMENTATION_KEY, DEFAULT_INSTRUMENTATION_KEY);
        arguments.addArgument(KEY_LIVE_METRICS, Boolean.toString(DEFAULT_LIVE_METRICS));
        arguments.addArgument(KEY_SAMPLERS_LIST, DEFAULT_SAMPLERS_LIST);
        arguments.addArgument(KEY_USE_REGEX_FOR_SAMPLER_LIST, Boolean.toString(DEFAULT_USE_REGEX_FOR_SAMPLER_LIST));

        return arguments;
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        testName = context.getParameter(KEY_TEST_NAME, DEFAULT_TEST_NAME);
        liveMetrics = context.getBooleanParameter(KEY_LIVE_METRICS, DEFAULT_LIVE_METRICS);
        samplersList = context.getParameter(KEY_SAMPLERS_LIST, DEFAULT_SAMPLERS_LIST).trim();
        useRegexForSamplerList = context.getBooleanParameter(KEY_USE_REGEX_FOR_SAMPLER_LIST, DEFAULT_USE_REGEX_FOR_SAMPLER_LIST);

        TelemetryConfiguration config = TelemetryConfiguration.createDefault();
        config.setInstrumentationKey(context.getParameter(KEY_INSTRUMENTATION_KEY));
        telemetryClient = new TelemetryClient(config);
        if (liveMetrics) {
            QuickPulse.INSTANCE.initialize(config);
        }

        samplersToFilter = new HashSet<String>();
        if (!useRegexForSamplerList) {
            String[] samplers = samplersList.split(SEPARATOR);
            samplersToFilter = new HashSet<String>();
            for (String samplerName : samplers) {
                samplersToFilter.add(samplerName);
            }
        }
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
        req.getContext().getOperation().setName(name);

        if (sr.getURL() != null) {
            req.setUrl(sr.getURL());
        }

        MapUtil.copy(properties, req.getProperties());
        telemetryClient.trackRequest(req);
    }

    @Override
    public void handleSampleResults(List<SampleResult> results, BackendListenerContext context) {

        boolean samplersToFilterMatch;
        for (SampleResult sr : results) {

            samplersToFilterMatch = samplersList.isEmpty() ||
                    (useRegexForSamplerList && sr.getSampleLabel().matches(samplersList)) ||
                    (!useRegexForSamplerList && samplersToFilter.contains(sr.getSampleLabel()));

            if (samplersToFilterMatch) {
                trackRequest(testName, sr);
            }
        }
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        samplersToFilter.clear();
        telemetryClient.flush();
        super.teardownTest(context);
    }
}
