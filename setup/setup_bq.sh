#!/bin/bash

gsutil cp ./setup/dimension_tables/*.json ${BQ_IMPORT_BUCKET}

bq --location=us mk \
    --dataset \
    ${PROJECT}:${DATASET}

for filename in ./setup/dimension_tables/*.json; do
    y=${filename%.json}

    bq load \
        --source_format=NEWLINE_DELIMITED_JSON \
        --autodetect \
        ${DATASET}.${y##*/} \
        gs://foglamp/imports/$(basename $filename) \
        ./setup/dimension_tables/${y##*/}

done