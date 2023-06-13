#!/bin/bash

openssl req -nodes -x509 -sha256 -newkey rsa:4096 \
  -keyout example.org.key \
  -out example.org.crt \
  -days 356 \
  -subj "/C=NL/ST=Zuid Holland/L=Rotterdam/O=ACME Corp/OU=IT Dept/CN=example.org"  \
  -addext "subjectAltName = DNS:localhost,DNS:example.org" 