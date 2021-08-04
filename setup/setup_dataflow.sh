#!/bin/bash
cd ~/dataflow-timeseries-iot-gas-demo
cd ./dataflow-events-iot
./gradlew clean execute \
     -Dexec.mainClass=com.foglamp_events.IoTStreamBigQueryEvents \
     -Dexec.args="--runner=DataflowRunner \
                  --project=${PROJECT} \
                  --region=${REGION} \
                  --stagingLocation=${STAGING_LOCATION} \
                  --tempLocation=${tempLocation} \
                  --gapSize=60 \
                  --inputTopic=projects/${PROJECT}/topics/foglamp-demo-events \
                  --outputTable=${PROJECT}:${DATASET}.measurements_raw_events"

cd ../
cd ./dataflow-raw
./gradlew clean execute \
     -Dexec.mainClass=com.foglamp.IoTStreamBigQueryRaw \
     -Dexec.args="--runner=DataflowRunner \
                  --project=${PROJECT} \
                  --region=${REGION} \
                  --stagingLocation=${STAGING_LOCATION} \
                  --tempLocation=${TEMP_LOCATION} \
                  --timerSize=30 \
                  --inputTopic=projects/${PROJECT}/topics/foglamp-demo \
                  --outputTopic=projects/${PROJECT}/topics/foglamp-demo-raw \
                  --outputEventTopic=projects/${PROJECT}/topics/foglamp-demo-events \
                  --inputTable=${PROJECT}.${DATASET}.event_definitions \
                  --outputTable=${PROJECT}:${DATASET}.measurements_raw"

cd ../
cd ./dataflow-timeseries-iot
./gradlew clean execute \
     -Dexec.mainClass=com.foglamp.IoTStreamBigQuery \
     -Dexec.args="--runner=DataflowRunner \
                  --project=${PROJECT} \
                  --region=${REGION} \
                  --stagingLocation=${STAGING_LOCATION} \
                  --tempLocation=${TEMP_LOCATION} \
                  --typeOneComputationsLengthInSecs=60 \
                  --typeTwoComputationsLengthInSecs=600 \
                  --inputTopic=projects/${PROJECT}/topics/foglamp-demo-raw \
                  --outputTable=${PROJECT}:${DATASET}.measurements_window_1min"

./gradlew clean execute \
     -Dexec.mainClass=com.foglamp.IoTStreamBigQuery \
     -Dexec.args="--runner=DataflowRunner \
                  --project=${PROJECT} \
                  --region=${REGION} \
                  --stagingLocation=${STAGING_LOCATION} \
                  --tempLocation=${TEMP_LOCATION} \
                  --typeOneComputationsLengthInSecs=600 \
                  --typeTwoComputationsLengthInSecs=3600 \
                  --inputTopic=projects/${PROJECT}/topics/foglamp-demo-raw \
                  --outputTable=${PROJECT}:${DATASET}.measurements_window_10min"