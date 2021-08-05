resource "google_storage_bucket" "foglamp_demo_main" {
    name     = "foglamp_demo_main"
    location = "${var.REGION}"
    uniform_bucket_level_access = true

    provisioner "local-exec" {
        command = <<-EOF
            #!/bin/bash
            export BQ_IMPORT_BUCKET=${google_storage_bucket.foglamp_demo_main.url}
            export PROJECT='${var.PROJECT}'
            export DATASET='foglamp_demo_test'
            chmod +x ./scripts/setup_bq.sh
            ./scripts/setup_bq.sh
        EOF
    }
}