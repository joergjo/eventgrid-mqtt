@description('Specifies the name prefix of all resources.')
param workspaceName string

@description('Specifies the location to deploy to.')
param location string

@description('Specifies the tags for all resources.')
param tags object = {}

resource logAnalyticsWorkspace 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: workspaceName
  location: location
  tags: tags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
  }
}

output workspaceId string = logAnalyticsWorkspace.id
