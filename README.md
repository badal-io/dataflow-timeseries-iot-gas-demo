# Dataflow IoT Timeseries Demo
## Overview
This repository provides a set of Apache Beam pipelines for processing streaming IoT sensor data from [FogLAMP](https://github.com/foglamp/FogLAMP) and writing them to BigQuery for downstream analytics.

![IoT Demo GCP Architecture](images/IoT_Demo_Diagram.png?raw=true "IoT Demo GCP Architecture")

## Getting Started
### Requirements
- A GCP project (to create one see [here](https://cloud.google.com/resource-manager/docs/creating-managing-projects))
- Java 8
- [Terraform](https://learn.hashicorp.com/tutorials/terraform/install-cli)

### Setting up the Demo
Executing Terraform will provision the following GCP resources:
- A virtual machine installed with FogLAMP, Prosys OPC UA server simulator, and Google Chrome Remote Desktop
- An IoT core registry and telemetry device
- Three Pub/Sub topics (```foglamp-demo```, ```foglamp-demo-raw```, and ```foglamp-demo-events```)
- Two GCS buckets (```foglamp_demo_main``` and ```foglamp_demo_dataflow```)
- A BigQuery Dataset (```foglamp_demp```) containing 5 tables (```assets```, ```device_connections```, ```devices```, ```event_definitions```, and ```paths```)
- Four Dataflow Jobs
Terraform will also create the necessary RSA keys to connect to the VM and authenticate the IoT Core device.
```:exclamation: The RSA keys generated will be stored unencrypted in your Terraform state file. In a production environment, generate your private keys outside of Terraform.```
1. Clone the repository to your local machine:
```git clone https://github.com/badal-io/dataflow-timeseries-iot-gas-demo.git```
2. Navigate to the Terraform directory:
```cd ./terragorm```
3. Edit the ```variables.tfvars``` file to configure the Terraform input variables with your values
4. Add your GCP credentials:
From the Cloud Console, download the JSON key file of an existing or new Service Account and store it on your local machine. Set the value of the environment variable ```GOOGLE_APPLICATION_CREDENTIALS``` to the location of the file:
```export GOOGLE_APPLICATION_CREDENTIALS={{path to service account JSON key}}```
Finally, execute ```gcloud auth login``` and follow the instructions to authenticate to GCP. 
5. Execute Terraform:
```
terraform init 
terraform apply -var-file="variables.tfvars
```
```:grey_exclamation: The execution will take approximately 7-8 minutes to deploy the configured resources```

## Apache Beam Pipelines
### [Processing of Raw IoT Sensor Data](https://github.com/badal-io/dataflow-timeseries-iot-gas-demo/tree/main/dataflow-raw)
The first pipeline is intended to be the point-of-entry for the raw IoT data. The pipeline consists of the following components:
- **Inputs**:
    1. Pub/Sub topic with raw sensor data from FogLAMP (unbounded main-input)
    2. BigQuery table with "event frame" definitions (bounded side-input)
- Format Pub/Sub messages to key/value pairs where they key is the IoT device-Id and the value is a BigQuery TableRow object
- Process the key/value pairs through a stateful, looping timer. The timer expires after a user-defined duration when the ```@ProcessElement DoFn``` hasn't received any new elements for a given key, thus enabling the detection of devices that have gone silent and potentially lost function. Upon expiry, the ``@OnTimer DoFn`` resets the timer for that key and outputs a TableRow with the key / device-id. 
- The ```EventFilter``` method describes a ```ParDo``` with two output ```PCollection```. It compares the key/value pairs against the conditions defined in the side-input table from BigQuery, and if they satisfy the conditions, the corresponding ```event_type``` field is appened to the TableRow and then they are outputted with an ```event_measurements``` tag, whereas all measurements are outputted with the ```all_measurements``` tag. The TableRows from the looping timer when a sensors has gone "silent" are also outputted here with the ```event_measurements``` tag.
- **Outputs**:
    1. The ```PCollection``` with the ```all_measurements``` tag is inserted to a BigQuery table containing all "raw" IoT sensor data
    2. The ```PCollection``` with the ```all_measurements``` tag is published to a Pub/Sub topic for downstream time-series processing
    3. The ```PCollection``` with the ```event_measurements``` tag is published to a Pub/Sub topic for downstream event processing

![Looping Stateful Timer (1)](images/looping_timer_1.png?raw=true "Looping Stateful Timer")

### [Processing of IoT Sensor Events](https://github.com/badal-io/dataflow-timeseries-iot-gas-demo/tree/main/dataflow-events-iot)
This pipeline is designed to process the sensor event data emitted from the first pipeline. The pipeline consists of the following components:
- **Inputs**:
    - Pub/Sub topic with sensor event data from the first pipeline
- Format Pub/Sub messages to key/value pairs where they key is the IoT ```device-Id # event_type``` and the value is a BigQuery TableRow object
- Process the key/value pairs through a stateful, looping timer. For every ```device-Id # event_type``` key, a timer and a random UUID ```event-Id``` are initialized and the ```event-Id``` is written to the ```ValueState``` interface. Every key/value pair of sensor events that are processed by the ```@ProcessElement DoFn``` reset the timer and read the current ```event-Id``` from the ```ValueState```, which is appened as an ```event-Id``` field to the TableRow before being outputted. After a user-defined duration without new elements for a given ```device-Id # event_type``` key, the timer for that key expires and a new ```event-Id``` is written to the ```ValueState``` replacing the old value.
- **Outputs**:
    - The ```PCollection``` is inserted to a BigQuery table containing all "event" IoT sensor data

![Looping Stateful Timer (2)](images/looping_timer_2.png?raw=true "Looping Stateful Timer")

### [Processing of Time-series Transforms](https://github.com/badal-io/dataflow-timeseries-iot-gas-demo/tree/main/dataflow-timeseries-iot)
The final pipeline is based on the Dataflow [Timeseries Streaming](https://github.com/GoogleCloudPlatform/dataflow-sample-applications) library to compute metrics across several time periods, such as the relative strength index (RSI) and moving average (MA). The pipeline consists of the following components:
- **Inputs**:
    - Pub/Sub topic with formatted sensor data from the first pipeline
- The custom method ```ParseTSDataPointFromPubSub``` transforms the Pub/Sub messages to the native ```TSDataPoint``` object of the Timeseries Library. The primary key is set to the ```device-Id```, whereas the secondary key is set to the ```property_measured``` in each data point (e.g. mass density, temperature, etc.).
- The ```GenerateComputations``` method of the TimeSeries Library is used to window the elements and compute the metrics declared in the pipeline options.
- Finally, the custom method ```TSAccumToRowPivot``` parses the ```PCollection``` with the computated metric values into a Row object.
- **Outputs**:
    - The ```PCollection``` is inserted to a BigQuery table containing all the timeseries-metrics data 