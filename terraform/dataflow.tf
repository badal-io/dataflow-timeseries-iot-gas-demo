resource "google_storage_bucket" "foglamp_demo_dataflow" {
    name     = "${var.project}-foglamp_demo_dataflow"
    location = var.region
    uniform_bucket_level_access = true
    force_destroy = true
    provisioner "local-exec" {
        command = <<-EOF
            #!/bin/bash
            export STAGING_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/staging'
            export TEMP_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/temp'
            export PROJECT='${var.project}'
            export REGION='${var.region}'
            export TEMPLATE_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/templates/events-iot'
            export TOPIC_EVENTS=${google_pubsub_topic.foglamp-demo-events.id}
            export DATASET='${var.dataset}'
            chmod +x ./scripts/setup_dataflow_events_iot.sh
            ./scripts/setup_dataflow_events_iot.sh
        EOF
    }
    provisioner "local-exec" {
        command = <<-EOF
            #!/bin/bash
            export STAGING_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/staging'
            export TEMP_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/temp'
            export PROJECT='${var.project}'
            export REGION='${var.region}'
            export TEMPLATE_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/templates/events-raw'
            export TOPIC_MAIN=${google_pubsub_topic.foglamp-demo.id}
            export TOPIC_RAW=${google_pubsub_topic.foglamp-demo-raw.id}
            export TOPIC_EVENTS=${google_pubsub_topic.foglamp-demo-events.id}
            export DATASET='${var.dataset}'
            chmod +x ./scripts/setup_dataflow_events_raw.sh
            ./scripts/setup_dataflow_events_raw.sh
        EOF
    }
    provisioner "local-exec" {
        command = <<-EOF
            #!/bin/bash
            export STAGING_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/staging'
            export TEMP_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/temp'
            export PROJECT='${var.project}'
            export REGION='${var.region}'
            export TEMPLATE_LOCATION='${google_storage_bucket.foglamp_demo_dataflow.url}/templates/timeseries-iot'
            export TOPIC_RAW=${google_pubsub_topic.foglamp-demo-raw.id}
            export DATASET='${var.dataset}'
            chmod +x ./scripts/setup_dataflow_events_timeseries.sh
            ./scripts/setup_dataflow_events_timeseries.sh
        EOF
    }
    depends_on = [google_pubsub_topic.foglamp-demo,
        google_pubsub_topic.foglamp-demo-raw,
        google_pubsub_topic.foglamp-demo-events,
        google_storage_bucket.foglamp_demo_main,
        google_project_iam_member.compute-account-iam-dataflow,
        google_project_iam_member.compute-account-iam-pubsub,
        google_project_iam_member.compute-account-iam-gcs,
        google_project_iam_member.compute-account-iam-cloudiot,
        google_project_iam_member.compute-account-iam-bq
    ]
}

resource "google_dataflow_job" "events-iot" {
    name = "events-iot"
    temp_gcs_location = "${google_storage_bucket.foglamp_demo_dataflow.url}/temp"
    template_gcs_path = "${google_storage_bucket.foglamp_demo_dataflow.url}/templates/events-iot"
    region = var.region
    on_delete = "cancel"
    skip_wait_on_job_termination=true
    service_account_email = "${var.project_number}-compute@developer.gserviceaccount.com"

    depends_on = [google_storage_bucket.foglamp_demo_dataflow]
}

resource "google_dataflow_job" "events-raw" {
    name = "events-raw"
    temp_gcs_location = "${google_storage_bucket.foglamp_demo_dataflow.url}/temp"
    template_gcs_path = "${google_storage_bucket.foglamp_demo_dataflow.url}/templates/events-raw"
    region = var.region
    on_delete = "cancel"
    skip_wait_on_job_termination=true
    service_account_email = "${var.project_number}-compute@developer.gserviceaccount.com"

    depends_on = [google_storage_bucket.foglamp_demo_dataflow]
}

resource "google_dataflow_job" "timeseries-iot" {
    name = "timeseries-iot"
    temp_gcs_location = "${google_storage_bucket.foglamp_demo_dataflow.url}/temp"
    template_gcs_path = "${google_storage_bucket.foglamp_demo_dataflow.url}/templates/timeseries-iot"
    region = var.region
    on_delete = "cancel"
    skip_wait_on_job_termination=true
    service_account_email = "${var.project_number}-compute@developer.gserviceaccount.com"

    depends_on = [google_storage_bucket.foglamp_demo_dataflow]
}
