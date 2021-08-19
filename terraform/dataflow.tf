resource "google_storage_bucket" "foglamp_demo_dataflow" {
    name     = "foglamp_demo_dataflow"
    location = "${var.REGION}"
    uniform_bucket_level_access = true

    provisioner "local-exec" {
        command = <<-EOF
            #!/bin/bash
            export STAGING_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/staging'
            export TEMP_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/temp'
            export PROJECT='${var.PROJECT}'
            export REGION='${var.REGION}'
            export TOPIC_MAIN=${google_pubsub_topic.foglamp-demo.id}
            export TOPIC_RAW=${google_pubsub_topic.foglamp-demo-raw.id}
            export TOPIC_EVENTS=${google_pubsub_topic.foglamp-demo-events.id}
            export DATASET='${var.DATASET}'
            chmod +x ./scripts/setup_dataflow.sh
            ./scripts/setup_dataflow.sh
        EOF
    }

    depends_on = [
        google_pubsub_topic.foglamp-demo,
        google_pubsub_topic.foglamp-demo-raw,
        google_pubsub_topic.foglamp-demo-events,
        google_storage_bucket.foglamp_demo_main
    ]
}