# Azure Event Grid MQTT Broker Sample Clients

This repository contains sample MQTT clients written in **Java**, **Go**, and **C#** that can be used as consumers and producers for Azure Event Grid MQTT broker. The samples demonstrate client certificate authentication and MQTT protocol usage with Azure Event Grid using both **MQTT v3.1.1** and **MQTT v5** (with OAuth2/Entra ID authentication).

## Overview

Azure Event Grid MQTT broker provides a fully managed MQTT messaging service that enables bidirectional communication between IoT devices and cloud applications. This repository includes sample clients in multiple programming languages to help you get started quickly.

📖 **Learn More**: [Azure Event Grid MQTT Overview](https://learn.microsoft.com/en-us/azure/event-grid/mqtt-overview)

## Prerequisites

This repository assumes you have:

1. **MQTT Knowledge**: Basic understanding of MQTT protocol and concepts
2. **Azure Event Grid Setup**: Knowledge of how to set up Azure Event Grid MQTT broker
3. **Azure Event Grid MQTT Broker**: A configured Azure Event Grid namespace with MQTT broker enabled
4. **Client Certificates**: Root CA and client certificates for authentication (see [Certificate Setup](#certificate-setup))

## Repository Structure

```
├── dotnet/                    # C# (.NET) MQTT client implementation
│   └── EventGridClient/
├── java/                      # Java MQTT client implementations
│   ├── mqttv3/
│   └── mqttv5/
├── go/                        # Go MQTT client implementation
│   └── clientv3/
├── scripts/                   # Certificate generation scripts
│   └── create-certs.sh
├── infra/                     # Azure infrastructure (Bicep templates)
└── README.md                  # This file
```

## Certificate Setup

### Important Prerequisite: Creating Client Certificates

One of the most important prerequisites is creating a root CA and client certificates to authenticate the clients. The `scripts/` folder contains a sample script that uses the **Step CLI** and **OpenSSL** to create sample self-signed certificates.

#### Prerequisites for Certificate Generation

1. **Step CLI**: Install the Step CLI tool
   ```bash
   # macOS
   brew install step
   
   # Linux/Windows
   # Download from https://github.com/smallstep/cli/releases
   ```

2. **OpenSSL**: Usually pre-installed on macOS/Linux, or install via package manager

#### Generate Certificates

Run the certificate generation script:

```bash
cd scripts/
chmod +x create-certs.sh
./create-certs.sh
```

This script will:
1. Create a root CA and intermediate CA for MQTT application samples
2. Generate client certificates for two devices (`client1-authn-ID` and `client2-authn-ID`)
3. Create PKCS12 bundles for Java compatibility
4. Display certificate thumbprints for Azure Event Grid client registration

#### Certificate Files Generated

- **PEM format**: `client1-authn-ID.pem`, `client2-authn-ID.pem` (certificates)
- **Private keys**: `client1-authn-ID.key`, `client2-authn-ID.key`
- **PKCS12 format**: `client1-authn-ID.p12`, `client2-authn-ID.p12` (for Java)
- **CA chain**: `ca_chain.crt`

## Client Implementations

### C# (.NET) Client

Located in `dotnet/EventGridClient/`, this implementation uses:
- **Framework**: .NET 8.0
- **MQTT Library**: MQTTnet

#### Configuration

The .NET client supports multiple configuration sources:
- `appsettings.json`
- User secrets
- Environment variables
- Command line arguments

#### Required Configuration Settings

```json
{
  "MQTT": {
    "BrokerFqdn": "your-namespace.region.eventgrid.azure.net",
    "Topic": "your/topic/path",
    "Username": "client1-authn-ID",
    "ClientId": "client1-authn-ID",
    "TlsCertFile": "client1-authn-ID.pem",
    "TlsKeyFile": "client1-authn-ID.key",
    "CleanSession": "true"
  }
}
```

#### Running the .NET Client

```bash
cd dotnet/EventGridClient/
dotnet run
```

### Java Clients

Located in `java/`, this repository provides **two Java implementations** demonstrating different MQTT versions and authentication methods:

#### MQTT v3.1.1 Client (`java/mqttv3/`)
- **Java version**: 8+
- **Build Tool**: Maven
- **MQTT Library**: Eclipse Paho MQTT Java Client
- **Authentication**: Client certificate authentication
- **Protocol**: MQTT v3.1.1

#### MQTT v5 Client (`java/mqttv5/`)
- **Java version**: 8+
- **Build Tool**: Maven  
- **MQTT Library**: Eclipse Paho MQTT Java Client
- **Authentication**: 
  - Client certificate authentication (same as MQTT v3.1.1)
  - OAuth2 with Microsoft Entra ID (Azure Active Directory)
- **Protocol**: MQTT v5
- **Features**: Showcases both certificate-based and token-based authentication with RBAC as described in the [Azure Event Grid MQTT Client Microsoft Entra Token and RBAC documentation](https://learn.microsoft.com/en-us/azure/event-grid/mqtt-client-microsoft-entra-token-and-rbac)

#### Building the Java Clients

```bash
# Build MQTT v3.1.1 client
cd java/mqttv3/
mvn clean package

# Build MQTT v5 client  
cd java/mqttv5/
mvn clean package
```

#### Running the MQTT v3.1.1 Client (Certificate Authentication)

```bash
cd java/mqttv3/
# Subscribe to topic
java -jar target/EventGridMqttSample-jar-with-dependencies.jar \
  -b your-namespace.region.eventgrid.azure.net \
  -u client2-authn-ID \
  -id client2-authn-ID \
  -t "your/topic/path" \
  -cc client2-authn-ID.p12 \
  -pw mypassword \
  -sub

# Publish to topic
java -jar target/EventGridMqttSample-jar-with-dependencies.jar \
  -b your-namespace.region.eventgrid.azure.net \
  -u client1-authn-ID \
  -id client1-authn-ID \
  -t "your/topic/path" \
  -m "Hello from Java MQTT v3.1.1!" \
  -cc client1-authn-ID.p12 \
  -pw mypassword \
  -pub
```

#### Running the MQTT v5 Client (Certificate Authentication)

```bash
cd java/mqttv5/
# Subscribe to topic using certificate authentication
java -jar target/EventGridMqttSample-jar-with-dependencies.jar \
  -b your-namespace.region.eventgrid.azure.net \
  -u client2-authn-ID \
  -id client2-authn-ID \
  -t "your/topic/path" \
  -cc client2-authn-ID.p12 \
  -pw mypassword \
  -sub

# Publish to topic using certificate authentication
java -jar target/EventGridMqttSample-jar-with-dependencies.jar \
  -b your-namespace.region.eventgrid.azure.net \
  -u client1-authn-ID \
  -id client1-authn-ID \
  -t "your/topic/path" \
  -m "Hello from Java MQTT v5 with certificates!" \
  -cc client1-authn-ID.p12 \
  -pw mypassword \
  -pub
```

#### Running the MQTT v5 Client (OAuth2/Entra ID Authentication)

```bash
cd java/mqttv5/
# Subscribe to topic using Entra ID authentication
java -jar target/EventGridMqttSample-jar-with-dependencies.jar \
  -b your-namespace.region.eventgrid.azure.net \
  -t "your/topic/path" \
  -id client1-authn-ID \
  -aad \
  -sub

# Publish to topic using Entra ID authentication
java -jar target/EventGridMqttSample-jar-with-dependencies.jar \
  -b your-namespace.region.eventgrid.azure.net \
  -t "your/topic/path" \
  -m "Hello from Java MQTT v5 with OAuth2!" \
  -id client2-authn-ID \
  -aad \
  -pub
```

**Note**: The `-aad` flag enables Azure Active Directory (Entra ID) authentication using the DefaultAzureCredential class from azure-identity. This automatically discovers credentials from various sources including:
- Environment variables (AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID)
- Managed Identity (when running on Azure)
- Azure CLI authentication
- Visual Studio Code authentication
- IntelliJ authentication

### Go Client

Located in `go/clientv3/`, this implementation uses:
- **Go Version**: Go 1.23 (modules enabled)
- **MQTT Library**: Eclipse Paho MQTT Go Client

#### Building the Go Client

```bash
cd go/clientv3/
go build -o mqtt-client
```

#### Running the Go Client

```bash
./mqtt-client \
  -fqdn your-namespace.region.eventgrid.azure.net \
  -username client1-authn-ID \
  -clientid client1-authn-ID \
  -topic "your/topic/path" \
  -certfile client1-authn-ID.pem \
  -keyfile client1-authn-ID.key
```

## Common Configuration Parameters

All clients support these common configuration parameters:

| Parameter | Description | Publisher Example | Subscriber Example |
|-----------|-------------|-------------------|-------------------|
| **Broker FQDN** | Event Grid namespace endpoint | `myns.region.eventgrid.azure.net` | `myns.region.eventgrid.azure.net` |
| **Port** | MQTT port (always 8883 for TLS) | `8883` | `8883` |
| **Username** | Client authentication name | `client1-authn-ID` | `client2-authn-ID` |
| **Client ID** | MQTT client identifier | `client1-authn-ID` | `client2-authn-ID` |
| **Topic** | MQTT topic path | `sensors/temperature` | `sensors/temperature` |
| **Certificate** | Client certificate file path | `client1-authn-ID.pem` | `client2-authn-ID.pem` |
| **Private Key** | Client private key file path | `client1-authn-ID.key` | `client2-authn-ID.key` |
| **Clean Session** | Start with clean session | `true` or `false` | `true` or `false` |

## Azure Event Grid Configuration

### Client Certificate Registration

Register the client certificates with Azure Event Grid using the thumbprints displayed by the certificate generation script:

```bash
az eventgrid namespace client create \
  --resource-group myResourceGroup \
  --namespace-name myEventGridNamespace \
  --name client1 \
  --authentication-name client1-authn-ID \
  --certificate-thumbprint "YOUR_CERTIFICATE_THUMBPRINT"

az eventgrid namespace client create \
  --resource-group myResourceGroup \
  --namespace-name myEventGridNamespace \
  --name client2 \
  --authentication-name client2-authn-ID \
  --certificate-thumbprint "YOUR_CERTIFICATE_THUMBPRINT"

```

### Topic Spaces and Permissions

Configure topic spaces and set appropriate permissions for your clients to publish and subscribe to topics.

## Troubleshooting

### Common Issues

1. **Certificate Authentication Failures**
   - Ensure certificates are properly generated and registered
   - Verify certificate thumbprints match Azure Event Grid registration
   - Check certificate expiration dates

2. **Connection Timeouts**
   - Verify network connectivity to Azure Event Grid endpoint
   - Check firewall rules allow outbound connections on port 8883
   - Ensure correct FQDN format

3. **Topic Permission Errors**
   - Verify topic spaces are configured correctly
   - Check client permissions for publish/subscribe operations
   - Ensure topic patterns match your topic names

## Infrastructure as Code

The `infra/` directory contains Bicep templates for deploying Azure Event Grid resources:

```bash
export AZ_LOCATION='northeurope'
export AZ_NAME_PREFIX='my-iot-demo'
export AZ_RESOURCE_GROUP='my-iot-rg'

cd infra/
./deploy.sh
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Additional Resources

- [Azure Event Grid MQTT Documentation](https://learn.microsoft.com/en-us/azure/event-grid/mqtt-overview)
- [MQTT 3.1.1 Protocol Specification](https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html)
- [MQTT 5.0 Protocl Specification](https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html)
- [Step CLI Documentation](https://smallstep.com/docs/step-cli/)
- [Eclipse Paho MQTT Clients](https://www.eclipse.org/paho/)
