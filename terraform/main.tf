provider "google" {
    project = var.project
    region  = var.region
    zone    = var.zone
}

terraform {
    backend "gcs" {
        bucket = "iot-poc-354821-terraform-state"
    }
}

resource "tls_private_key" "google_compute_engine_ssh" {
    algorithm = "RSA"
    rsa_bits  = 4096
}

resource "tls_private_key" "google_compute_engine_ssh_v2" {
    algorithm = "RSA"
    rsa_bits  = 4096
}

resource "tls_private_key" "foglamp_rsa" {
    algorithm = "RSA"
    rsa_bits  = 2048
}

resource "local_file" "foglamp_rsa_public" {
    content = tls_private_key.foglamp_rsa.public_key_pem
    filename = "./foglamp_keys/rsa_public.pem"
}

resource "local_file" "foglamp_rsa_private" {
    content = tls_private_key.foglamp_rsa.private_key_pem
    filename = "./foglamp_keys/rsa_private.pem"
}