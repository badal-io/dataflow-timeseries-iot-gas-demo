# TODO Create custom role for default compute service account
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