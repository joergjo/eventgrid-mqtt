#!/bin/bash

subscription_id=$(az account show --query id -o tsv)
resource_group=messaging-weu
namespace=mqtt-demo
client=client-1
topicSpaceName=test
permissionBinding1=permissionbinding1
permissionBinding2=permissionbinding2

az resource create --resource-type Microsoft.EventGrid/namespaces \
  --id /subscriptions/${subscription_id}/resourceGroups/${resource_group}/providers/Microsoft.EventGrid/namespaces/${namespace} \
  --is-full-object \
  --api-version 2023-06-01-preview \
  --properties @./resources/namespace.json

az resource create --resource-type Microsoft.EventGrid/namespaces/clients \
  --id /subscriptions/${subscription_id}/resourceGroups/${resource_group}/providers/Microsoft.EventGrid/namespaces/${namespace}/clients/${client} \
  --api-version 2023-06-01-preview \
  --properties @./resources/client1.json

az resource create --resource-type Microsoft.EventGrid/namespaces/topicSpaces \
  --id /subscriptions/${subscription_id}/resourceGroups/${resource_group}/providers/Microsoft.EventGrid/namespaces/${namespace}/topicSpaces/${topicSpaceName} \
  --api-version 2023-06-01-preview \
  --properties @./resources/topicspace.json

az resource create --resource-type Microsoft.EventGrid/namespaces/permissionBindings \
  --id /subscriptions/${subscription_id}/resourceGroups/${resource_group}/providers/Microsoft.EventGrid/namespaces/${namespace}/permissionBindings/${permissionBinding1} \
  --api-version 2023-06-01-preview \
  --properties @./resources/permissionbinding1.json

az resource create --resource-type Microsoft.EventGrid/namespaces/permissionBindings \
  --id /subscriptions/${subscription_id}/resourceGroups/${resource_group}/providers/Microsoft.EventGrid/namespaces/${namespace}/permissionBindings/${permissionBinding2} \
  --api-version 2023-06-01-preview \
  --properties @./resources/permissionbinding2.json