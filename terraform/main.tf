provider "google" {
    project = var.project
    region  = var.region
    zone    = var.zone
}

terraform {
    backend "gcs" {
        bucket = "<project>-terraform-state"
    }
}

resource "tls_private_key" "google_compute_engine_ssh" {
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

resource "google_compute_network" "default" {
    name = "default"
}

resource "google_compute_firewall" "default" {
    name    = "default"
    network = google_compute_network.default.name

    allow {
        protocol = "icmp"
    }

    allow {
        protocol = "tcp"
        ports    = ["22", "8081"]
    }

    source_tags = ["web"]
    source_ranges = ["0.0.0.0/0"]
}

resource "google_compute_address" "static" {
    name = "ipv4-address"
}

resource "google_compute_instance" "instance_with_ip" {
    name         = "${var.project}-foglamp-demo-instance"
    machine_type = "e2-standard-2"
    can_ip_forward = true
    tags = ["http-server","https-server"]

    boot_disk {
        initialize_params {
            image = "ubuntu-1804-bionic-v20210623"
            size = 100
            type = "pd-standard"
        }
    }

    network_interface {
        network = "default"
        access_config {
            nat_ip = google_compute_address.static.address
        }
    }

    metadata = {
        ssh-keys = "${var.user}:${tls_private_key.google_compute_engine_ssh.public_key_openssh}"
    }

    provisioner "remote-exec"{
        inline = [
            "cd ~/",
            "mkdir scripts",
            "mkdir foglamp_keys"
        ]
        connection {
            type        = "ssh"
            host        = google_compute_instance.instance_with_ip.network_interface.0.access_config.0.nat_ip
            user        = var.user
            private_key = tls_private_key.google_compute_engine_ssh.private_key_pem
        }
    }

    provisioner "file" {
        source = "./scripts/"
        destination = "scripts"
        connection {
            type        = "ssh"
            host        = google_compute_instance.instance_with_ip.network_interface.0.access_config.0.nat_ip
            user        = var.user
            private_key = tls_private_key.google_compute_engine_ssh.private_key_pem
        }
    }

    provisioner "file" {
        source = "./foglamp_keys/"
        destination = "foglamp_keys"
        connection {
            type        = "ssh"
            host        = google_compute_instance.instance_with_ip.network_interface.0.access_config.0.nat_ip
            user        = var.user
            private_key = tls_private_key.google_compute_engine_ssh.private_key_pem
        }
    }

    provisioner "remote-exec" {
        inline = [
            "cd ~/",
            "chmod +x ~/scripts/setup_vm.sh",
            "~/scripts/setup_vm.sh"
        ]
        connection {
            type        = "ssh"
            host        = google_compute_instance.instance_with_ip.network_interface.0.access_config.0.nat_ip
            user        = var.user
            private_key = tls_private_key.google_compute_engine_ssh.private_key_pem
        }
    }
    depends_on = [
        google_project_iam_member.compute-account-iam-bq,
        google_project_iam_member.compute-account-iam-cloudiot,
        google_project_iam_member.compute-account-iam-dataflow,
        google_project_iam_member.compute-account-iam-gcs,
        google_project_iam_member.compute-account-iam-pubsub
    ]
}

output "internal_ip" {
    value = google_compute_instance.instance_with_ip.network_interface.0.network_ip
}