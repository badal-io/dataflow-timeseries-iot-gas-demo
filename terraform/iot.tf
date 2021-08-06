resource "google_pubsub_topic" "foglamp-demo" {
    name = "foglamp-demo"
}

resource "google_pubsub_topic" "foglamp-demo-raw" {
    name = "foglamp-demo-raw"
}

resource "google_pubsub_topic" "foglamp-demo-events" {
    name = "foglamp-demo-events"
}

resource "google_cloudiot_registry" "foglamp-demo-registry" {
    name     = "foglamp-demo-registry"

    event_notification_configs {
        pubsub_topic_name = google_pubsub_topic.foglamp-demo.id
    }

}

resource "google_cloudiot_device" "foglamp-demo-device" {
    name     = "foglamp-demo-device"
    registry = google_cloudiot_registry.foglamp-demo-registry.id

    credentials {
        public_key {
            format = "RSA_PEM"
            key = tls_private_key.foglamp_rsa.public_key_pem
        }
    }

    depends_on = [tls_private_key.foglamp_rsa]
    
}