#!/bin/bash
cd ../
cd ./dataflow-timeseries-iot
./gradlew clean execute \
     -Dexec.mainClass=com.foglamp.IoTStreamBigQuery \
     -Dexec.args="--runner=DataflowRunner \
                  --project=${PROJECT} \
                  --region=${REGION} \
                  --stagingLocation=${STAGING_LOCATION} \
                  --templateLocation=${TEMPLATE_LOCATION} \
                  --tempLocation=${TEMP_LOCATION} \
                  --typeOneComputationsLengthInSecs=60 \
                  --typeTwoComputationsLengthInSecs=600 \
                  --inputTopic=${TOPIC_RAW} \
                  --outputTable=${PROJECT}:${DATASET}.measurements_window_1min"