#!/bin/bash
required_vars=("AZ_NAME_PREFIX" "AZ_RESOURCE_GROUP" "AZ_LOCATION")

for var in "${required_vars[@]}"; do
  if [ -z "${!var}" ]; then
    echo "$var is not set."
    exit 1
  fi
done

echo "Creating resource group $AZ_RESOURCE_GROUP in $AZ_LOCATION..."

az group create \
  --resource-group "$AZ_RESOURCE_GROUP" \
  --location "$AZ_LOCATION" \
  --output none

# Exit if the resource group creation failed
if [ $? -ne 0 ]; then
  echo "Failed to create resource group $AZ_RESOURCE_GROUP in $AZ_LOCATION."
  exit 1
fi

echo "Deploying infrastructure..."

timestamp=$(date +%s)
mqtt_broker_fqdn=$(az deployment group create \
  --resource-group "$AZ_RESOURCE_GROUP" \
  --name "mqtt-infra-$timestamp" \
  --template-file main.bicep \
  --parameters namePrefix="$AZ_NAME_PREFIX"  \
  --query properties.outputs.mqttBrokerFQDN.value \
  --output tsv)

echo "Deployment succeeded. MQTT broker FQDN: $mqtt_broker_fqdn"