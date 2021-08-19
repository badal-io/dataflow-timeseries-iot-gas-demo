#!/bin/bash

gsutil cp ./dimension_tables/*.json ${BQ_IMPORT_BUCKET}

bq --location=us mk \
    --dataset \
    ${PROJECT}:${DATASET}

for filename in ./dimension_tables/*.json; do
    y=${filename%.json}

    bq load \
        --source_format=NEWLINE_DELIMITED_JSON \
        --autodetect \
        ${DATASET}.${y##*/} \
        ${BQ_IMPORT_BUCKET}/$(basename $filename) \
        ./dimension_tables/${y##*/}

done