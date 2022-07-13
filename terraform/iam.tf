# TODO Create custom role for default compute service account
resource "google_project_iam_member" "compute-account-iam-cloudiot" {
  project = var.PROJECT
  role = "roles/cloudiot.editor"
  member = "serviceAccount:${var.PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
  depends_on = [google_compute_instance.instance_with_ip]
}

resource "google_project_iam_member" "compute-account-iam-bq" {
  project = var.PROJECT
  role = "roles/bigquery.dataEditor"
  member = "serviceAccount:${var.PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
  depends_on = [google_compute_instance.instance_with_ip]
}

resource "google_project_iam_member" "compute-account-iam-dataflow" {
  project = var.PROJECT
  role = "roles/dataflow.worker"
  member = "serviceAccount:${var.PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
  depends_on = [google_compute_instance.instance_with_ip]
}

resource "google_project_iam_member" "compute-account-iam-gcs" {
  project = var.PROJECT
  role = "roles/storage.admin"
  member = "serviceAccount:${var.PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
  depends_on = [google_compute_instance.instance_with_ip]
}