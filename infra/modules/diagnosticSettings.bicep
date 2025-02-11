@description('Specifies the name of the Diagnostic Settings namespace.')
param settingsName string

@description('Specifies the name of the Event Grid namespace.')
param namespaceName string

@description('Specifies the name of the Log Analytics workspace.')
param workspaceName string

resource logAnalyticsWorkspace 'Microsoft.OperationalInsights/workspaces@2023-09-01' existing = {
  name: workspaceName
}

resource eventGridNamespace 'Microsoft.EventGrid/namespaces@2024-12-15-preview' existing = {
  name: namespaceName
}

resource diagnosticSettings 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = {
  name: settingsName
  scope: eventGridNamespace
  properties: {
    workspaceId: logAnalyticsWorkspace.id
    logs: [
      {
        enabled: true
        category: 'SuccessfulMqttConnections'
      }
      {
        enabled: true
        category: 'FailedMqttConnections'
      }
      {
        enabled: true
        category: 'FailedMqttSubscriptionOperations'
      }
      {
        enabled: true
        category: 'FailedMqttPublishedMessages'
      }
      {
        enabled: true
        category: 'MqttDisconnections'
      }
    ]
  }
}
