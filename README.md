# Dataflow IoT Timeseries Demo
## Overview
gcloud compute ssh --project="sandbox-keyera-poc" --zone="us-central1-a" foglamp-demo-instance-test

This repository provides a set of Apache Beam pipelines for processing streaming IoT sensor data from [FogLAMP](https://github.com/foglamp/FogLAMP) and writing them to BigQuery for downstream analytics.

![IoT Demo GCP Architecture](images/IoT_Demo_Diagram.png?raw=true "IoT Demo GCP Architecture")

## Getting Started
### Requirements
- A GCP project (to create one see [here](https://cloud.google.com/resource-manager/docs/creating-managing-projects))
- Java 8
- [Terraform](https://learn.hashicorp.com/tutorials/terraform/install-cli)

### Setting up the Demo
<b>Executing Terraform will provision the following GCP resources:</b>
- A virtual machine installed with FogLAMP, Prosys OPC UA server simulator, and Google Chrome Remote Desktop
- An IoT core registry and telemetry device
- Three Pub/Sub topics (```foglamp-demo```, ```foglamp-demo-raw```, and ```foglamp-demo-events```)
- Two GCS buckets (```foglamp_demo_main``` and ```foglamp_demo_dataflow```)
- A BigQuery Dataset (```foglamp_demp```) containing 5 tables (```assets```, ```device_connections```, ```devices```, ```event_definitions```, and ```paths```)
- Three Dataflow Jobs  

Terraform will also create the necessary RSA keys to connect to the VM and authenticate the connection between FogLAMP and the IoT Core device.  

:exclamation: The RSA keys generated will be stored unencrypted in your Terraform state file. In a production environment, generate your private keys outside of Terraform.  

<b>Follow the following steps to deploy the Demo resources using Terraform:</b>

1. Clone the repository to your local machine:  
```
git clone https://github.com/badal-io/dataflow-timeseries-iot-gas-demo.git
```
2. Navigate to the Terraform directory:  
```
cd ./terraform
```
3. Edit the ```variables.tfvars``` file to configure the Terraform input variables with your values
4. To add your GCP credentials, navigate to the Cloud Console and download the JSON key file of an existing or new Service Account and store it on your local machine. Set the value of the environment variable ```GOOGLE_APPLICATION_CREDENTIALS``` to the location of the file:  
```
export GOOGLE_APPLICATION_CREDENTIALS={{path to service account JSON key}}
```
Finally, execute ```gcloud auth login``` and follow the instructions to authenticate to GCP.     
5. Run Terraform:
```
terraform init 
terraform apply -var-file="variables.tfvars
```
:grey_exclamation: The deployment will take approximately 6-7 minutes.  

<b> Once Terraform has finished deploying the GCP resources needed for the Demo, you can start setting up FogLAMP:</b>

1. Connect to the VM through the Google Cloud Console or the ```gcloud``` command-line tool:  
```
gcloud compute ssh --project=${PROJECT} --zone=${ZONE} VM_NAME 
```
2. After you connect, use the browser in your local machine to navigate to [Google Chrome Remote Desktop](https://remotedesktop.google.com/headless)  
3. Click on ```Begin``` > ```Next``` > ```Authorize```. The page should display a command line for Debian Linux that looks like the following:  
```
DISPLAY= /opt/google/chrome-remote-desktop/start-host \
    --code="4/xxxxxxxxxxxxxxxxxxxxxxxx" \
    --redirect-url="https://remotedesktop.google.com/_/oauthredirect" \
    --name=$(hostname)
```  
4. Copy the command and paste it to the terminal of your VM in the SSH window that's connected to your instance, and then run the command. Follow the steps to setup a pin. Ignore errors like ```No net_fetcher``` or ```Failed to read```.  
5. Navigate back to [Chrome's Remote Access](https://remotedesktop.google.com/access). You should see the VM's name listed under "Remote Devices". Click on it and enter your pin when prompted. You are now connected to the desktop environment of your VM.
6. On the Google Cloud Console, navigate to the Compute Engine and locate your VM. Note the internal IP allocated to your machine.
7. From the desktop environment of your VM, navigate to ```/opt/prosys-opc-ua-simulation-server``` and click on the ```Prosys OPC UA Simulation Server``` icon. Once the Server Status changes to "Running", copy the "Connection Address (UA TCP)" as you will need this later. 
8. In the OPC UA server, click on the second tab "Objects". Remove the sample nodes, and add a new object node:  
![Object Node](images/object_node.png?raw=true "Object Node")  
9. Then add a variable node under the object node:
![Variable Node](images/variable_node.png?raw=true "Variable Node")     
:exclamation: Take note of the node IDs as you will need them later on.  
10. Open the browser of your VM and navigate to ```http://{{ Your VM's internal IP}}```. Your are now accessing the FogLAMP dashboard UI.  
11. Using the menu bar on the left side of the UI, click on "South" and then click on "Add+" in the upper right of the South Services screen. Select "opcua" from the list and provide a unique name for the asset. Click on "Next".
12. Copy the Connection Address of your OPC UA server to the "OPCUA Server URL" field, and ender the Node Id of your object node to the "OPCUA Object Subscriptions":
![FogLAMP South](images/foglamp_south.png?raw=true "FogLAMP South") 
13. Click on "Next" and <b>unselect "Enabled"</b> for now.
14. From the "South Services" menu, click on your asset and then click on "Applications+". From the Plugin list select "metadata" and click on "Next".
15. Here you can enter useful metadata associated with your sensor, such as location, configuration version, etc. For the demo we will define the device version:
![Metadata](images/metadata.png?raw=true "Metadata")  
16. Click on "Done" to enable the Metadata plugin.
17. Back on the configuration menu of your asset, click on "Applications+" and from the Plugin list select "rename".
18. Select "datapoint" as the "Operation" and set the "Find" field value to the Node Id of your OPC UA variable. Replace it with the actual property being measured, e.g. "temperature":
![Rename](images/rename.png?raw=true "Rename") 
19. Using the menu bar on the left side of the UI, click on "North" and then click on "Add+" in the upper right of the North Services screen. Select "GCP" from the list and provide a unique name for the asset. Click on "Next".
20. Enter your Project ID and region, and the following default values for the Registry ID, Device ID, and Key Name. Terraform has already configured the private key required to connect FogLAMP and GCP IoT Core, so all that's required is to enter the default key name:  
![FogLAMP North](images/foglamp_north.png?raw=true "FogLAMP North")  
21. Click on "Next" to enable the GCP plugin.
22. Finally, back to the "South Service" menu, click on your asset and select "Enabled" to activate it.
23. After a few moments, you should be able to see the number of messages that have been read/sent through FogLAMP:
![FogLAMP Final](images/foglamp_final.png?raw=true "FogLAMP Final")  
### Exploring the Data
Once FogLAMP is transmitting the OPC UA data to the IoT Core, the downstream Pub/Sub topics and Dataflow Jobs will stream them to BigQuery.  
To explore the data:
1. Go to the BigQuery console
2. Look for the ```foglamp_demo``` dataset, where you will have access to the following tables:
![BigQuery](images/bigquery.png?raw=true "BigQuery")  
3. The ```measurements_raw``` table is where the raw IoT data are landed, whereas the IoT data processed with the Dataflow [Timeseries Streaming](https://github.com/GoogleCloudPlatform/dataflow-sample-applications) library are inserted to the ```measurements_window_1min```. Note that you can configure the Terraform configuration to deploy as many as Timeseries Dataflow jobs you wish to cover different windowing periods (e.g. 1 min, 10 min, etc.). 
### Simulating Event Frames
One of the features of this demo is the capturing of abnormal device behaviour in the form of events. Let's do the following example:
1. In the desktop environment of your VM, go to the OPC UA server and stop the simulation by clicking on the "Stop" button in the "Objects" tab:
![OPC UA Stop](images/opcua_stop.png?raw=true "OPC UA Stop")
2. Back to BigQuery, you will now have a table ```measurements_raw_events``` where the outage of the sensor is captured in real-time for as long as the outage lasts:
![Events](images/opcua_stop.png?raw=true "Events")  
What about custom events? The ```event_definitions``` table allows a user to define custom events for all or specific devices and measured properties:
![Event Definitions](images/event_definitions.png?raw=true "Event Definitions")

## Apache Beam Pipelines
### [Processing of Raw IoT Sensor Data](https://github.com/badal-io/dataflow-timeseries-iot-gas-demo/tree/main/dataflow-raw)
The first pipeline is intended to be the point-of-entry for the raw IoT data. The pipeline consists of the following components:
- **Inputs**:
    1. Pub/Sub topic with raw sensor data from FogLAMP (unbounded main-input)
    2. BigQuery table with "event frame" definitions (bounded side-input)
- Format Pub/Sub messages to key/value pairs where they key is the IoT device-Id and the value is a BigQuery TableRow object
- Process the key/value pairs through a stateful, looping timer. The timer expires after a user-defined duration when the ```@ProcessElement DoFn``` hasn't received any new elements for a given key, thus enabling the detection of devices that have gone silent and potentially lost function. Upon expiry, the ``@OnTimer DoFn`` resets the timer for that key and outputs a TableRow with the key / device-id. 
- The ```EventFilter``` method describes a ```ParDo``` with two output ```PCollection```. It compares the key/value pairs against the conditions defined in the side-input table from BigQuery, and if they satisfy the conditions, the corresponding ```event_type``` field is appened to the TableRow and then they are outputted with an ```event_measurements``` tag, whereas all measurements are outputted with the ```all_measurements``` tag. The TableRows from the looping timer when a sensor has gone "silent" are also outputted here with the ```event_measurements``` tag.
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