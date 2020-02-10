# jmeter-backend-azure

![Java CI](https://github.com/adrianmo/jmeter-backend-azure/workflows/Java%20CI/badge.svg)

A JMeter plug-in that enables you to send test results to Azure Application Insights.

## Overview

### Description

JMeter Backend Azure is a JMeter plugin enabling you to send test results to an Azure Application Insights. This plugin is inspired in the [Elasticsearch](https://github.com/delirius325/jmeter-elasticsearch-backend-listener) and [Kafka](https://github.com/rahulsinghai/jmeter-backend-listener-kafka) backend listener plugins. 

### Build

- To build the artifact, execute below Maven command. Make sure `JAVA_HOME` is set properly.

```bash
mvn clean package
```

- Move the resulting JAR to your `JMETER_HOME/lib/ext`.

```bash
mv target/jmeter.backendlistener.azure-VERSION.jar $JMETER_HOME/lib/ext/
```

- Restart JMeter

### Configuring jmeter-backend-azure plug-in

- In your **Test Pan**, right click on **Thread Group** > Add > Listener > Backend Listener
- Choose `io.github.adrianmo.jmeter.backendlistener.azure.AzureBackendClient` as `Backend Listener Implementation`.
- Specify the Application Insights instrumentation key as a parameter as shown in image below: 

![Screenshot of configuration](docs/configuration.png "Screenshot of configuration")

## Contributing

Feel free to contribute by branching and making pull requests, or simply by suggesting ideas through the "Issues" tab.
