#!/bin/bash
step ca init --deployment-type standalone --name MqttAppSamplesCA --dns localhost --address 127.0.0.1:443 --provisioner MqttAppSamplesCAProvisionera
step certificate create client1-authn-ID client1-authn-ID.pem client1-authn-ID.key --ca ~/.step/certs/intermediate_ca.crt --ca-key ~/.step/secrets/intermediate_ca_key --no-password --insecure --not-after 2400h
step certificate create client2-authn-ID client2-authn-ID.pem client2-authn-ID.key --ca ~/.step/certs/intermediate_ca.crt --ca-key ~/.step/secrets/intermediate_ca_key --no-password --insecure --not-after 2400h

step certificate fingerprint client1-authn-ID.pem
step certificate fingerprint client2-authn-ID.pem