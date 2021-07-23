# dataflow-timeseries-iot-gas-demo


To do:

1) Validate RSI calculations
2) Understand window settings
3) Expand schema for multiple measured properties
4) Develop airflow for creating the GCS folders


./gradlew clean execute \
     -Dexec.mainClass=com.foglamp.IoTStreamBigQuery \
     -Dexec.args="--runner=DataflowRunner \
                  --project=sandbox-keyera-poc \
                  --region=us-central1 \
                  --stagingLocation=gs://foglamp/dataflow/staging \
                  --tempLocation=gs://foglamp/dataflow/temp \
                  --inputTopic=projects/sandbox-keyera-poc/topics/dataflow-timeseries \
                  --outputTable=sandbox-keyera-poc:dataflow_IoT.test_table"

export GOOGLE_APPLICATION_CREDENTIALS="/home/michail/compute-key.json"


./gradlew clean execute \
     -Dexec.mainClass=com.foglamp_events.IoTStreamBigQueryEvents \
     -Dexec.args="--runner=DataflowRunner \
                  --project=sandbox-keyera-poc \
                  --region=us-central1 \
                  --stagingLocation=gs://foglamp/dataflow/staging2 \
                  --tempLocation=gs://foglamp/dataflow/temp2 \
                  --inputTopic=projects/sandbox-keyera-poc/topics/dataflow-timeseries \
                  --outputTable=sandbox-keyera-poc:dataflow_IoT.test_table_events"

export GOOGLE_APPLICATION_CREDENTIALS="/home/michail/compute-key.json"