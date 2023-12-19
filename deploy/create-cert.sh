#!/bin/bash

openssl req -nodes -x509 -sha256 -newkey rsa:4096 \
  -keyout ../example.org.key \
  -out ../example.org.crt \
  -days 356 \
  -subj "/CN=example.org"  \
  -addext "subjectAltName = DNS:localhost,DNS:example.org" 

cat ../example.org.crt ../example.org.key > ../example.org.pem