provider "google" {
    project = "${var.PROJECT}"
    region  = "us-central1"
    zone    = "us-central1-a"
}

resource "google_compute_address" "static" {
  name = "ipv4-address"
}

resource "google_compute_instance" "instance_with_ip" {
    name         = "foglamp-demo-instance-test-2"
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

    provisioner "file" {
        source = "/home/michail/dataflow-timeseries-iot-gas-demo/terraform/scripts/setup_vm.sh"
        destination = "~/setup_vm.sh"
        connection {
            type        = "ssh"
            host        = google_compute_address.static.address
            user        = "${var.USER}"
            private_key = file("/home/michail/.ssh/id_rsa")
        }
    }

    provisioner "remote-exec" {
        inline = [
            "export CODE='${var.CODE}'",
            "export PIN='${var.PIN}'",
            "chmod +x ~/setup_vm.sh"
        ]
        connection {
            type        = "ssh"
            host        = google_compute_address.static.address
            user        = "${var.USER}"
            private_key = file("/home/michail/.ssh/id_rsa")
        }
    }
}

variable "PROJECT" {
  type = string
}

variable "CODE" {
  type = string
}

variable "PIN" {
  type = string
}

variable "USER" {
  type = string
}