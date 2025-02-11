@description('Specifies the common name prefix for all resources')
@minLength(3)
param namePrefix string

@description('Specifies the location for all resources')
param location string = resourceGroup().location

var namespaceName = '${namePrefix}-broker'
var workspaceName = '${namePrefix}-logs'
var settingsName = 'log-to-workspace'

module monitoring 'modules/monitoring.bicep' = {
  name: 'monitoring'
  params: {
    workspaceName: workspaceName
    location: location
  }
}

module broker 'modules/eventgrid.bicep' = {
  name: 'broker'
  params: {
    namespaceName: namespaceName
    location: location
  }
}

module diagnosticSettings 'modules/diagnosticSettings.bicep' = {
  name: 'diagnosticSettings'
  params: {
    settingsName: settingsName
    namespaceName: namespaceName
    workspaceName: workspaceName
  }
}

output mqttBrokerFQDN string = broker.outputs.mqttBrokerFQDN
