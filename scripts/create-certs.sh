#!/bin/bash

# Create a root CA and an intermediate CA for MQTT application samples
step ca init --deployment-type standalone --name MqttAppSamplesCA --dns localhost --address 127.0.0.1:443 --provisioner MqttAppSamplesCAProvisioner

# Create client certificates for authentication for two devices
step certificate create client1-authn-ID client1-authn-ID.pem client1-authn-ID.key --ca ~/.step/certs/intermediate_ca.crt --ca-key ~/.step/secrets/intermediate_ca_key --no-password --insecure --not-after 2400h
step certificate create client2-authn-ID client2-authn-ID.pem client2-authn-ID.key --ca ~/.step/certs/intermediate_ca.crt --ca-key ~/.step/secrets/intermediate_ca_key --no-password --insecure --not-after 2400h

# Create a PKCS12 bundle for each client certificate (used by the Java sample)
openssl pkcs12 -export -out client1-authn-ID.p12 -inkey client1-authn-ID.key -in client1-authn-ID.pem -password pass:mypassword
openssl pkcs12 -export -out client2-authn-ID.p12 -inkey client2-authn-ID.key -in client2-authn-ID.pem -password pass:mypassword

# Show thumbprints for client registration with Azure Event Grid
step certificate fingerprint client1-authn-ID.pem
step certificate fingerprint client2-authn-ID.pem