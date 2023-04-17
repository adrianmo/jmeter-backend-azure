package io.github.adrianmo.jmeter.backendlistener;

public enum DataLoggingOption {
    Always("Always"),
    OnFailure("OnFailure"),
    Never("Never");

    private final String value;

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
        if (value == "true")
            return DataLoggingOption.Always;
        else if (value == "false")
            return DataLoggingOption.Never;
        else
            // Default value
            return OnFailure;
    }
}
