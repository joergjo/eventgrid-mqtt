@description('Specifies the name of the Event Grid namespace.')
param namespaceName string

@description('Specifies the location for the Event Grid namespace.')
param location string 

@description('Specifies the capacity for the Event Grid namespace.')
param capacity int = 1

@description('Specifies the maximum session expiry in hours for the Event Grid namespace.')
param maxSessionExpiryInHours int = 1

@description('Specifies the maximum client sessions per authentication name for the Event Grid namespace.')
param maxClientSessionsPerAuthenticationName int = 1

@description('Specifies the tags for all resources.')
param tags object = {}

resource eventGridNamespace 'Microsoft.EventGrid/namespaces@2024-12-15-preview' = {
  name: namespaceName
  location: location
  tags: tags
  sku: {
    name: 'Standard'
    capacity: capacity
  }
  identity: {
    type: 'SystemAssigned' 
  }
  properties: {
    topicSpacesConfiguration: {
      state: 'Enabled'
      maximumSessionExpiryInHours: maxSessionExpiryInHours
      maximumClientSessionsPerAuthenticationName: maxClientSessionsPerAuthenticationName
    }
    isZoneRedundant: true
    publicNetworkAccess: 'Enabled'
    inboundIpRules: []
  }
}

resource cloudToDeviceTopicSpace 'Microsoft.EventGrid/namespaces/topicSpaces@2024-12-15-preview' = {
  parent: eventGridNamespace
  name: 'cloudToDevice'
  properties: {
    topicTemplates: [
      'device/+/commands/#'
      'device/+/commandresponses/#'
    ]
  }
}

resource deviceToCloudTopicSpace 'Microsoft.EventGrid/namespaces/topicSpaces@2024-12-15-preview' = {
  parent: eventGridNamespace
  name: 'deviceToCloud'
  properties: {
    topicTemplates: [
      'device/+/telemetry/#'
      'device/+/telemetryresponses/#'
    ]
  }
}

resource cloudToDevicePublisher 'Microsoft.EventGrid/namespaces/permissionBindings@2024-12-15-preview' = {
  parent: eventGridNamespace
  name: 'cloudToDevicePublisher'
  properties: {
    topicSpaceName: cloudToDeviceTopicSpace.name
    permission: 'Publisher'
    clientGroupName: '$all'
  }
}

resource cloudToDeviceSubscriber 'Microsoft.EventGrid/namespaces/permissionBindings@2024-12-15-preview' = {
  parent: eventGridNamespace
  name: 'cloudToDeviceSubscriber'
  properties: {
    topicSpaceName: cloudToDeviceTopicSpace.name
    permission: 'Subscriber'
    clientGroupName: '$all'
  }
}

resource deviceToCloudPublisher 'Microsoft.EventGrid/namespaces/permissionBindings@2024-12-15-preview' = {
  parent: eventGridNamespace
  name: 'deviceToCloudPublisher'
  properties: {
    topicSpaceName: deviceToCloudTopicSpace.name
    permission: 'Publisher'
    clientGroupName: '$all'
  }
}

resource deviceToCloudSubscriber 'Microsoft.EventGrid/namespaces/permissionBindings@2024-12-15-preview' = {
  parent: eventGridNamespace
  name: 'deviceToCloudSubscriber'
  properties: {
    topicSpaceName: deviceToCloudTopicSpace.name
    permission: 'Subscriber'
    clientGroupName: '$all'
  }
}

output mqttBrokerFQDN string = eventGridNamespace.properties.topicSpacesConfiguration.hostname
