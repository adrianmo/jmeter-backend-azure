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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
    private static final String KEY_CONNECTION_STRING = "connectionString";
    private static final String KEY_LIVE_METRICS = "liveMetrics";
    private static final String KEY_SAMPLERS_LIST = "samplersList";
    private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "useRegexForSamplerList";
    private static final String KEY_CUSTOM_PROPERTIES_PREFIX = "ai.";
    private static final String KEY_HEADERS_PREFIX = "aih.";
    private static final String KEY_RESPONSE_HEADERS = "responseHeaders";
    private static final String KEY_LOG_RESPONSE_DATA = "logResponseData";
    private static final String KEY_LOG_SAMPLE_DATA = "logSampleData";

    /**
     * Default argument values.
     */
    private static final String DEFAULT_TEST_NAME = "jmeter";
    private static final String DEFAULT_CONNECTION_STRING = "";
    private static final boolean DEFAULT_LIVE_METRICS = true;
    private static final String DEFAULT_SAMPLERS_LIST = "";
    private static final boolean DEFAULT_USE_REGEX_FOR_SAMPLER_LIST = false;
    private static final DataLoggingOption DEFAULT_LOG_RESPONSE_DATA = DataLoggingOption.OnFailure;
    private static final DataLoggingOption DEFAULT_LOG_SAMPLE_DATA = DataLoggingOption.OnFailure;

    /**
     * Separator for samplers list.
     */
    private static final String SEPARATOR = ";";

    /**
     * Truncated length of the request and response data.
     */
    private static final int MAX_DATA_LENGTH = 1024;

    /**
     * Application Insights telemetry client.
     */
    private TelemetryClient telemetryClient;

    /**
     * Name of the test.
     */
    private String testName;

    /**
     * Custom properties.
     */
    private Map<String, String> customProperties = new HashMap<String, String>();

    /**
     * Recording response headers.
     */
    private String[] responseHeaders = {};

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

    /**
     * Whether to log the response data to the backend
     */
    private DataLoggingOption logResponseData;

    /**
     * Whether to log the sample data to the backend
     */
    private DataLoggingOption logSampleData;

    public AzureBackendClient() {
        super();
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument(KEY_TEST_NAME, DEFAULT_TEST_NAME);
        arguments.addArgument(KEY_CONNECTION_STRING, DEFAULT_CONNECTION_STRING);
        arguments.addArgument(KEY_LIVE_METRICS, Boolean.toString(DEFAULT_LIVE_METRICS));
        arguments.addArgument(KEY_SAMPLERS_LIST, DEFAULT_SAMPLERS_LIST);
        arguments.addArgument(KEY_USE_REGEX_FOR_SAMPLER_LIST, Boolean.toString(DEFAULT_USE_REGEX_FOR_SAMPLER_LIST));
        arguments.addArgument(KEY_LOG_RESPONSE_DATA, DEFAULT_LOG_RESPONSE_DATA.getValue());
        arguments.addArgument(KEY_LOG_SAMPLE_DATA, DEFAULT_LOG_SAMPLE_DATA.getValue());

        return arguments;
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        testName = context.getParameter(KEY_TEST_NAME, DEFAULT_TEST_NAME);
        liveMetrics = context.getBooleanParameter(KEY_LIVE_METRICS, DEFAULT_LIVE_METRICS);
        samplersList = context.getParameter(KEY_SAMPLERS_LIST, DEFAULT_SAMPLERS_LIST).trim();
        useRegexForSamplerList = context.getBooleanParameter(KEY_USE_REGEX_FOR_SAMPLER_LIST,
                DEFAULT_USE_REGEX_FOR_SAMPLER_LIST);
        logResponseData = DataLoggingOption
                .fromString(context.getParameter(KEY_LOG_RESPONSE_DATA, DEFAULT_LOG_RESPONSE_DATA.getValue()));
        logSampleData = DataLoggingOption
                .fromString(context.getParameter(KEY_LOG_SAMPLE_DATA, DEFAULT_LOG_SAMPLE_DATA.getValue()));

        Iterator<String> iterator = context.getParameterNamesIterator();
        while (iterator.hasNext()) {
            String paramName = iterator.next();
            if (paramName.startsWith(KEY_CUSTOM_PROPERTIES_PREFIX)) {
                customProperties.put(paramName, context.getParameter(paramName));
            } else if (paramName.equals(KEY_RESPONSE_HEADERS)) {
                responseHeaders = context.getParameter(KEY_RESPONSE_HEADERS).trim().toLowerCase()
                        .split("\\s*".concat(SEPARATOR).concat("\\s*"));
            }
        }

        TelemetryConfiguration config = TelemetryConfiguration.createDefault();
        String instrumentationKey = context.getParameter(KEY_INSTRUMENTATION_KEY);
        if (instrumentationKey != null) {
            log.warn("'instrumentationKey' is deprecated, use 'connectionString' instead");
            config.setInstrumentationKey(instrumentationKey);
        }

        String connectionString = context.getParameter(KEY_CONNECTION_STRING);
        if (connectionString != null) {
            config.setConnectionString(connectionString);
        }

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
        properties.putAll(customProperties);
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

        for (String header : responseHeaders) {
            Pattern pattern = Pattern.compile("^".concat(header).concat(":(.*)$"),
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sr.getResponseHeaders());
            if (matcher.find()) {
                properties.put(KEY_HEADERS_PREFIX.concat(header), matcher.group(1).trim());
            }
        }

        Date timestamp = new Date(sr.getTimeStamp());
        Duration duration = new Duration(sr.getTime());
        RequestTelemetry req = new RequestTelemetry(name, timestamp, duration, sr.getResponseCode(),
                sr.isSuccessful());
        req.getContext().getOperation().setName(name);

        if (sr.getURL() != null) {
            req.setUrl(sr.getURL());
        }

        if (sr.getSamplerData() != null && ((logSampleData == DataLoggingOption.Always) ||
                (logSampleData == DataLoggingOption.OnFailure && !sr.isSuccessful()))) {

            if (sr.getDataType() == SampleResult.TEXT) {
                String samplerData;
                if (sr.getSamplerData().length() > MAX_DATA_LENGTH) {
                    log.warn("Sample data is too long, truncating it to {} characters", MAX_DATA_LENGTH);
                    samplerData = sr.getSamplerData().substring(0, MAX_DATA_LENGTH) + "...[TRUNCATED]";
                } else {
                    samplerData = sr.getSamplerData();
                }
                properties.put("SampleData", samplerData);
            } else {
                log.warn("Sample data is in binary format, cannot log it");
                properties.put("SampleData", "[BINARY DATA]");
            }
        }

        if (logResponseData == DataLoggingOption.Always ||
                (logResponseData == DataLoggingOption.OnFailure && !sr.isSuccessful())) {
            String responseData;
            if (sr.getResponseDataAsString().length() > MAX_DATA_LENGTH) {
                log.warn("Response data is too long, truncating it to {} characters", MAX_DATA_LENGTH);
                responseData = sr.getResponseDataAsString().substring(0, MAX_DATA_LENGTH) + "...[TRUNCATED]";
            } else {
                responseData = sr.getResponseDataAsString();
            }
            properties.put("ResponseData", responseData);
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
