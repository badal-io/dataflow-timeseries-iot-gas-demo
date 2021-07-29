#!/bin/bash

export DATASET="foglamp_demo_test"
export PROJECT="sandbox-keyera-poc"
export REGION="us-central1"
export stagingLocation="gs://foglamp/dataflow/staging"
export tempLocation="gs://foglamp/dataflow/temp"

export GOOGLE_APPLICATION_CREDENTIALS="/home/michail/compute-key.json"

#gcloud pubsub topics create foglamp-demo --project=${PROJECT}
export topicIoT="foglamp-demo"

#gcloud pubsub topics create foglamp-demo-raw --project=${PROJECT}
#gcloud pubsub topics create foglamp-demo-events --project=${PROJECT}

cd ./dataflow-events-iot
./gradlew clean execute \
     -Dexec.mainClass=com.foglamp_events.IoTStreamBigQueryEvents \
     -Dexec.args="--runner=DataflowRunner \
                  --project=${PROJECT} \
                  --region=${REGION} \
                  --stagingLocation=${stagingLocation} \
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
                  --stagingLocation=${stagingLocation} \
                  --tempLocation=${tempLocation} \
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
                  --stagingLocation=${stagingLocation} \
                  --tempLocation=${tempLocation} \
                  --typeOneComputationsLengthInSecs=60 \
                  --typeTwoComputationsLengthInSecs=600 \
                  --inputTopic=projects/${PROJECT}/topics/foglamp-demo-raw \
                  --outputTable=${PROJECT}:${DATASET}.measurements_window_1min"

./gradlew clean execute \
     -Dexec.mainClass=com.foglamp.IoTStreamBigQuery \
     -Dexec.args="--runner=DataflowRunner \
                  --project=${PROJECT} \
                  --region=${REGION} \
                  --stagingLocation=${stagingLocation} \
                  --tempLocation=${tempLocation} \
                  --typeOneComputationsLengthInSecs=600 \
                  --typeTwoComputationsLengthInSecs=3600 \
                  --inputTopic=projects/${PROJECT}/topics/foglamp-demo-raw \
                  --outputTable=${PROJECT}:${DATASET}.measurements_window_10min"