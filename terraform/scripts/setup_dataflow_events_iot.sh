#!/bin/bash
cd ../
cd ./dataflow-events-iot
./gradlew clean execute \
     -Dexec.mainClass=com.foglamp_events.IoTStreamBigQueryEvents \
     -Dexec.args="--runner=DataflowRunner \
                  --project=${PROJECT} \
                  --region=${REGION} \
                  --templateLocation=${TEMPLATE_LOCATION} \
                  --stagingLocation=${STAGING_LOCATION} \
                  --tempLocation=${TEMP_LOCATION} \
                  --gapSize=60 \
                  --inputTopic=${TOPIC_EVENTS} \
                  --outputTable=${PROJECT}:${DATASET}.measurements_raw_events"
