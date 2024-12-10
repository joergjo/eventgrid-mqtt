using System.Security.Cryptography.X509Certificates;
using Microsoft.Extensions.Configuration;
using MQTTnet;
using MQTTnet.Client;

var builder = new ConfigurationBuilder()
    .SetBasePath(Directory.GetCurrentDirectory())
    .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true)
    .AddUserSecrets<Program>()
    .AddEnvironmentVariables()
    .AddCommandLine(args);
var configuration = builder.Build();

string[] keys = ["MQTT:BrokerFqdn", "MQTT:Username", "MQTT:ClientId", "MQTT:TlsCertFile", "MQTT:TlsKeyFile"];
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

var certificate =
    new X509Certificate2(X509Certificate2.CreateFromPemFile(
        configuration["Mqtt:TlsCertFile"]!,
        configuration["Mqtt:TlsKeyFile"])
        .Export(X509ContentType.Pkcs12));

var mqttClient = new MqttFactory().CreateMqttClient();

var connAck = await mqttClient!.ConnectAsync(new MqttClientOptionsBuilder()
    .WithTcpServer(configuration["Mqtt:BrokerFqdn"], 8883)
    .WithClientId(configuration["Mqtt:ClientId"])
    .WithCredentials(configuration["Mqtt:Username"], string.Empty)  //use client authentication name in the username
    .WithTlsOptions(options => 
    {
        options.UseTls(true);
        options.WithClientCertificates([certificate]);
    })
    .Build());

Console.WriteLine($"Client connected: {mqttClient.IsConnected} with CONNACK: {connAck.ResultCode}");

mqttClient.ApplicationMessageReceivedAsync +=
    async m => await Console.Out.WriteAsync($"Received message on topic: '{m.ApplicationMessage.Topic}' with content: '{m.ApplicationMessage.ConvertPayloadToString()}'\n\n");

if (configuration.GetValue("Subscribe", false))
{
    await mqttClient.SubscribeAsync("contosotopics/topic1", MQTTnet.Protocol.MqttQualityOfServiceLevel.AtLeastOnce);
    var suback = await mqttClient.SubscribeAsync("contosotopics/topic1");
    suback.Items.ToList().ForEach(s => Console.WriteLine($"subscribed to '{s.TopicFilter.Topic}' with '{s.ResultCode}'"));
}

Console.WriteLine("Sending messages");
while (true)
{
    var puback = await mqttClient.PublishStringAsync("contosotopics/topic1", $"Hello world at {DateTimeOffset.UtcNow:o}!");
    Console.WriteLine(puback.ReasonString);
    await Task.Delay(5000);
}