# dataflow-timeseries-iot-gas-demo

./gradlew clean execute \
     -Dexec.mainClass=com.foglamp_events.IoTStreamBigQueryRaw \
     -Dexec.args="--runner=DataflowRunner \
                  --project=sandbox-keyera-poc \
                  --region=us-central1 \
                  --stagingLocation=gs://foglamp/dataflow/staging2 \
                  --tempLocation=gs://foglamp/dataflow/temp2 \
                  --inputTopic=projects/sandbox-keyera-poc/topics/foglamp-demo \
                  --outputTopic=projects/sandbox-keyera-poc/topics/foglamp-demo-raw \
                  --outputEventTopic=projects/sandbox-keyera-poc/topics/foglamp-demo-events \
                  --inputTable=sandbox-keyera-poc.foglamp_demo.event_definitions \
                  --outputTable=sandbox-keyera-poc:foglamp_demo.measurements_raw"

./gradlew clean execute \
     -Dexec.mainClass=com.foglamp_events.IoTStreamBigQuery \
     -Dexec.args="--runner=DataflowRunner \
                  --project=sandbox-keyera-poc \
                  --region=us-central1 \
                  --stagingLocation=gs://foglamp/dataflow/staging2 \
                  --tempLocation=gs://foglamp/dataflow/temp2 \
                  --typeOneComputationsLengthInSecs=60 \
                  --typeTwoComputationsLengthInSecs=600 \
                  --inputTopic=projects/sandbox-keyera-poc/topics/foglamp-demo-raw \
                  --outputTable=sandbox-keyera-poc:foglamp_demo.measurements_window_1min"

./gradlew clean execute \
     -Dexec.mainClass=com.foglamp_events.IoTStreamBigQuery \
     -Dexec.args="--runner=DataflowRunner \
                  --project=sandbox-keyera-poc \
                  --region=us-central1 \
                  --stagingLocation=gs://foglamp/dataflow/staging2 \
                  --tempLocation=gs://foglamp/dataflow/temp2 \
                  --typeOneComputationsLengthInSecs=600 \
                  --typeTwoComputationsLengthInSecs=3600 \
                  --inputTopic=projects/sandbox-keyera-poc/topics/foglamp-demo-raw \
                  --outputTable=sandbox-keyera-poc:foglamp_demo.measurements_window_10min"

./gradlew clean execute \
     -Dexec.mainClass=com.foglamp_events.IoTStreamBigQueryEvents \
     -Dexec.args="--runner=DataflowRunner \
                  --project=sandbox-keyera-poc \
                  --region=us-central1 \
                  --stagingLocation=gs://foglamp/dataflow/staging2 \
                  --tempLocation=gs://foglamp/dataflow/temp2 \
                  --gapSize=60 \
                  --inputTopic=projects/sandbox-keyera-poc/topics/foglamp-demo-events \
                  --outputTable=sandbox-keyera-poc:foglamp_demo.measurements_raw_events"

export GOOGLE_APPLICATION_CREDENTIALS="/home/michail/compute-key.json"