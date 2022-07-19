#!/bin/bash
cd ../
cd ./dataflow-raw
./gradlew clean execute \
     -Dexec.mainClass=com.foglamp.IoTStreamBigQueryRaw \
     -Dexec.args="--runner=DataflowRunner \
                  --project=${PROJECT} \
                  --region=${REGION} \
                  --templateLocation=${TEMPLATE_LOCATION} \
                  --stagingLocation=${STAGING_LOCATION} \
                  --tempLocation=${TEMP_LOCATION} \
                  --timerSize=30 \
                  --inputTopic=${TOPIC_MAIN} \
                  --outputTopic=${TOPIC_RAW} \
                  --outputEventTopic=${TOPIC_EVENTS} \
                  --inputTable=${PROJECT}.${DATASET}.event_definitions \
                  --outputTable=${PROJECT}:${DATASET}.measurements_raw"