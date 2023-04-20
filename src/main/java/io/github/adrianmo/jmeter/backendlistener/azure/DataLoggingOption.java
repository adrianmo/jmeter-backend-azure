package io.github.adrianmo.jmeter.backendlistener.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum DataLoggingOption {
    Always("Always"),
    OnFailure("OnFailure"),
    Never("Never");

    private final String value;
    private static final Logger log = LoggerFactory.getLogger(AzureBackendClient.class);

    DataLoggingOption(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DataLoggingOption fromString(String value) {
        for (DataLoggingOption option : DataLoggingOption.values()) {
            if (option.value.equalsIgnoreCase(value)) {
                return option;
            }
        }

        // Conditions to provide backwards compatibility
        if (value == "true") {
            log.warn("Logging value 'true' is deprecated, replacing with 'Always'");
            return DataLoggingOption.Always;
        } else if (value == "false") {
            log.warn("Logging value 'false' is deprecated, replacing with 'Never'");
            return DataLoggingOption.Never;
        } else if (value != "") {
            log.warn("Logging value '{}' is not valid, defaulting to 'OnFailure'", value);
        }

        return OnFailure;
    }
}
