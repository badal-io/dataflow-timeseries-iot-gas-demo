# Dataflow IoT Timeseries Demo
## Overview
This repository provides a set of Apache Beam pipelines for processing streaming times-series IoT sensor data from [FogLAMP](https://github.com/foglamp/FogLAMP) and writing them to BigQuery for downstream analytics.
## Apache Beam Pipelines
### [Processing of Raw IoT Sensor Data](https://github.com/foglamp/FogLAMP)
The first pipeline is intended to be the point-of-entry for the raw IoT data. The pipeline consists of the following components:
- **Inputs**:
    1. Pub/Sub topic with raw sensor data from FogLAMP (unbounded main-input)
    2. BigQuery table with "event frame" definitions (bounded side-input)
- Format Pub/Sub messages to key/value pairs where they key is the IoT device-Id and the value is a BigQuery TableRow object
- Process the key/value pairs through a stateful, looping timer. The timer expires after a user-defined duration when the ```@ProcessElement DoFn``` hasn't received any new elements for a given key, thus enabling the detection of devices that have gone silent and potentially lost function. Upon expiry, the ``@OnTimer DoFn`` resets the timer for that key and outputs a TableRow with the key / device-id. 
- The ```EventFilter``` method describes a ```ParDo``` with two output ```PCollection```. It compares the key/value pairs against the conditions defined in the side-input table from BigQuery, and if they satisfy the conditions, the corresponding ```event_type``` field is appened to the TableRow and then they are outputted with an ```event_measurements``` tag, whereas all measurements are outputted with the ```all_measurements``` tag. The TableRows from the looping timer when a sensors has gone "silent" are also outputted here with the ```event_measurements``` tag.
- **Outputs**:
&nbsp;&nbsp;&nbsp;&nbsp;1\. The ```PCollection``` with the ```all_measurements``` tag is inserted to a BigQuery table containing all "raw" IoT sensor data
&nbsp;&nbsp;&nbsp;&nbsp;2\. The ```PCollection``` with the ```all_measurements``` tag is published to a Pub/Sub topic for downstream time-series processing
&nbsp;&nbsp;&nbsp;&nbsp;2\. The ```PCollection``` with the ```event_measurements``` tag is published to a Pub/Sub topic for downstream event processing

![Looping Stateful Timer (1)](images/looping_timer_1.png?raw=true "Looping Stateful Timer")

### [Processing of IoT Sensor Events](https://github.com/foglamp/FogLAMP)
This pipeline is designed to process the sensor event data emitted from the first pipeline. The pipeline consists of the following components:
- **Inputs**:
&nbsp;&nbsp;&nbsp;&nbsp;1\. Pub/Sub topic with sensor event data from the first pipeline
- Format Pub/Sub messages to key/value pairs where they key is the IoT ```device-Id # event_type``` and the value is a BigQuery TableRow object
- Process the key/value pairs through a stateful, looping timer. For every ```device-Id # event_type``` key, a timer and a random UUID ```event-Id``` are initialized and the ```event-Id``` is written to the ```ValueState``` interface. Every key/value pair of sensor events that are processed by the ```@ProcessElement DoFn``` reset the timer and read the current ```event-Id``` from the ```ValueState```, which is appened as an ```event-Id``` field to the TableRow before being outputted. After a user-defined duration without new elements for a given ```device-Id # event_type``` key, the timer for that key expires and a new ```event-Id``` is written to the ```ValueState``` replacing the old value.
- **Outputs**:
&nbsp;&nbsp;&nbsp;&nbsp;1\. The ```PCollection``` is inserted to a BigQuery table containing all "event" IoT sensor data

### [Processing of Time-series Transforms](https://github.com/foglamp/FogLAMP)
The final pipeline is based on the Dataflow [Timeseries Streaming]() library to compute metrics across several time periods, such as the relative strength index (RSI) and moving average (MA). The pipeline consists of the following components:
- **Inputs**:
&nbsp;&nbsp;&nbsp;&nbsp;1\. Pub/Sub topic with formatted sensor data from the first pipeline
- The custom method ```ParseTSDataPointFromPubSub``` transforms the Pub/Sub messages to the native ```TSDataPoint``` object of the Timeseries Library. The primary key is set to the ```device-Id```, whereas the secondary key is set to the ```property_measured``` in each data point (e.g. mass density, temperature, etc.).
- The ```GenerateComputations``` method of the TimeSeries Library is used to window the elements and compute the metrics declared in the pipeline options.
- Finally, the custom method ```TSAccumToRowPivot``` parses the ```PCollection``` with the computated metric values into a Row object.
- **Outputs**:
&nbsp;&nbsp;&nbsp;&nbsp;1\. The ```PCollection``` is inserted to a BigQuery table containing all the timeseries-metrics data 