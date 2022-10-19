# TODO Create custom role for default compute service account
resource "google_service_account" "foglamp_service_account" {
  account_id   = "foglamp"
  display_name = "Foglamp"
  description = "SA used by foglamp north plugin to publish in pub sub"
}

resource "google_service_account" "looker_service_account" {
  account_id   = "looker"
  display_name = "looker"
  description = "SA used by looker to connect and interact with BQ"
}

#resource "google_service_account_key" "foglamp-sa-key" {
#  service_account_id = google_service_account.foglamp_service_account.name
#}

resource "google_project_iam_member" "foglamp-pubsub" {
  project = var.project
  role = "roles/pubsub.editor"
  member = "serviceAccount:${google_service_account.foglamp_service_account.email}"
}

resource "google_project_iam_member" "looker-bq" {
  project = var.project
  role = "roles/bigquery.admin"
  member = "serviceAccount:${google_service_account.looker_service_account.email}"
}

resource "google_project_iam_member" "compute-account-iam-cloudiot" {
  project = var.project
  role = "roles/cloudiot.editor"
  member = "serviceAccount:${var.project_number}-compute@developer.gserviceaccount.com"
}

resource "google_project_iam_member" "compute-account-iam-bq" {
  project = var.project
  role = "roles/bigquery.admin"
  member = "serviceAccount:${var.project_number}-compute@developer.gserviceaccount.com"
}

resource "google_project_iam_member" "compute-account-iam-dataflow" {
  project = var.project
  role = "roles/dataflow.worker"
  member = "serviceAccount:${var.project_number}-compute@developer.gserviceaccount.com"
}

resource "google_project_iam_member" "compute-account-iam-gcs" {
  project = var.project
  role = "roles/storage.admin"
  member = "serviceAccount:${var.project_number}-compute@developer.gserviceaccount.com"
}

resource "google_project_iam_member" "compute-account-iam-pubsub" {
  project = var.project
  role = "roles/pubsub.admin"
  member = "serviceAccount:${var.project_number}-compute@developer.gserviceaccount.com"
}

resource "google_iap_tunnel_instance_iam_binding" "enable_iap" {
  instance = google_compute_instance.instance_with_ip.id
  project  = var.project
  zone     = var.zone
  role     = "roles/iap.tunnelResourceAccessor"
  members  = [
    "serviceAccount:${var.project_number}-compute@developer.gserviceaccount.com",
    "serviceAccount:terraform@iot-poc-354821.iam.gserviceaccount.com"
  ]
}