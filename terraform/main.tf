provider "google" {
    project = "${var.PROJECT}"
    region  = "${var.REGION}"
    zone    = "${var.ZONE}"
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
    content = "${tls_private_key.foglamp_rsa.public_key_pem}"
    filename = "./foglamp_keys/rsa_public.pem"
}

resource "local_file" "foglamp_rsa_private" {
    content = "${tls_private_key.foglamp_rsa.private_key_pem}"
    filename = "./foglamp_keys/rsa_private.pem"
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
            host        = google_compute_instance.instance_with_ip.network_interface.0.access_config.0.nat_ip
            user        = "${var.USER}"
            private_key = tls_private_key.google_compute_engine_ssh.private_key_pem
        }
    }

    provisioner "file" {
        source = "./foglamp_keys"
        destination = "~/foglamp_keys"
        connection {
            type        = "ssh"
            host        = google_compute_instance.instance_with_ip.network_interface.0.access_config.0.nat_ip
            user        = "${var.USER}"
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
            user        = "${var.USER}"
            private_key = tls_private_key.google_compute_engine_ssh.private_key_pem
        }
    }
}