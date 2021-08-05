provider "google" {
    project = "${var.PROJECT}"
    region  = "${var.REGION}"
    zone    = "${var.ZONE}"
}

resource "tls_private_key" "google_compute_engine_ssh" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "google_compute_address" "static" {
  name = "ipv4-address-demo"
}

resource "google_compute_instance" "instance_with_ip" {
    name         = "foglamp-demo-instance-test"
    machine_type = "e2-standard-2"

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
        ssh-keys = "${var.USER}:${tls_private_key.google_compute_engine_ssh.public_key_openssh}"
    }

    provisioner "file" {
        source = "/home/michail/dataflow-timeseries-iot-gas-demo/terraform/scripts"
        destination = "~/scripts"
        connection {
            type        = "ssh"
            host        = google_compute_address.static.address #google_compute_instance.instance_with_ip.network_interface.0.access_config.0.nat_ip
            user        = "${var.USER}"
            private_key = tls_private_key.google_compute_engine_ssh.private_key_pem
        }
    }

    provisioner "remote-exec" {
        inline = [
            "chmod +x ~/scripts/setup_vm.sh",
            "~/scripts/setup_vm.sh"
        ]
        connection {
            type        = "ssh"
            host        = google_compute_address.static.address
            user        = "${var.USER}"
            private_key = tls_private_key.google_compute_engine_ssh.private_key_pem
        }
    }
}

variable "PROJECT" {
  type = string
}

variable "USER" {
    type = string
}

variable "REGION" {
    type = string
}

variable "ZONE" {
    type = string
}