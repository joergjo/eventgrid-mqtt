using System.Security.Cryptography.X509Certificates;
using Microsoft.Extensions.Configuration;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Exceptions;
using MQTTnet.Protocol;

var builder = new ConfigurationBuilder()
    .SetBasePath(Directory.GetCurrentDirectory())
    .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true)
    .AddUserSecrets<Program>()
    .AddEnvironmentVariables()
    .AddCommandLine(args);
var configuration = builder.Build();

string[] keys = ["MQTT:BrokerFqdn", "MQTT:Topic", "MQTT:Username", "MQTT:ClientId", "MQTT:TlsCertFile", "MQTT:TlsKeyFile", "MQTT:CleanSession"];
var isValidConfig = true;

foreach (var key in keys)
{
    var setting = configuration[key];
    if (setting is null or { Length: 0 })
    {
        Console.WriteLine($"Please configure the {key} setting in appsettings.json or via user secrets or environment variables.");
        isValidConfig = false;
    }
}

if (!isValidConfig)
{
    Console.WriteLine("Exiting due to missing configuration.");
    return;
}

var cts = new CancellationTokenSource();
Console.CancelKeyPress += (sender, e) =>
{
    e.Cancel = true;
    cts.Cancel();
};

var certificate =
    new X509Certificate2(X509Certificate2.CreateFromPemFile(
        configuration["Mqtt:TlsCertFile"]!,
        configuration["Mqtt:TlsKeyFile"])
        .Export(X509ContentType.Pkcs12));

using var mqttClient = new MqttFactory().CreateMqttClient();

var connAck = await mqttClient!.ConnectAsync(new MqttClientOptionsBuilder()
    .WithTcpServer(configuration["Mqtt:BrokerFqdn"], 8883)
    .WithClientId(configuration["Mqtt:ClientId"])
    .WithCredentials(configuration["Mqtt:Username"], string.Empty)  //use client authentication name in the username
    .WithTlsOptions(options =>
    {
        options.UseTls(true);
        options.WithClientCertificates([certificate]);
    })
    .WithCleanSession(configuration["Mqtt:CleanSession"] == "true")
    .Build());

Console.WriteLine($"Client connected: {mqttClient.IsConnected} with CONNACK: {connAck.ResultCode}");

mqttClient.DisconnectedAsync +=
    async e => await Console.Out.WriteLineAsync($"Disconnected from the broker with reason: {e.Reason}");

string topic = configuration["MQTT:Topic"]!;

if (configuration.GetValue("Subscribe", false))
{
    mqttClient.ApplicationMessageReceivedAsync +=
        async m => await Console.Out.WriteLineAsync($"Received message on topic: '{m.ApplicationMessage.Topic}' with content: '{m.ApplicationMessage.ConvertPayloadToString()}'\n\n");
    Console.WriteLine("Subscribing to topic '{0}'", topic);
    var suback = await mqttClient.SubscribeAsync(topic, MqttQualityOfServiceLevel.AtLeastOnce, cancellationToken: cts.Token);
    suback.Items.ToList().ForEach(s => Console.WriteLine($"subscribed to '{s.TopicFilter.Topic}' with '{s.ResultCode}'"));
}

if (configuration.GetValue("Publish", false))
{
    var message = configuration.GetValue("Message", "Hello MQTT from .NET");
    Console.WriteLine("Publishing messages to topic'{0}'", topic);
    try
    {
        for (int i = 1; !cts.IsCancellationRequested; i++)
        {
            var payload = $"{message} #{i} !";
            var puback = await mqttClient.PublishStringAsync(topic, payload);
            if (!puback.IsSuccess)
            {
                Console.WriteLine($"Failed to publish message: {payload}");
            }
            await Task.Delay(2000);
        }
    }
    catch (MqttClientNotConnectedException)
    {
        Console.WriteLine("Client is not longer connected");
    }
}

// Prevent the application from exiting immediately
cts.Token.WaitHandle.WaitOne();
Console.WriteLine("Application is terminating...");