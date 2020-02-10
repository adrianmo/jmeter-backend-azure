package io.github.adrianmo.jmeter.backendlistener.azure;

import org.apache.jmeter.config.Arguments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestAzureBackendClient {

    private static AzureBackendClient client;

    @BeforeAll
    public static void setUp() {
        client = new AzureBackendClient();
    }

    @Test
    public void testGetDefaultParameters() {
        Arguments args = client.getDefaultParameters();
        assertNotNull(args);
    }

}
