using System.Security.Cryptography.X509Certificates;
using MQTTnet;
using MQTTnet.Client;

string hostname = "mqtt-demo.westeurope-1.ts.eventgrid.azure.net";
string clientId = "client1-session1";  //client ID can be the session identifier.  A client can have multiple sessions using username and clientId.
string pemFile = @"./example.org.crt";  //Provide your client certificate .cer.pem file path
string keyFile = @"./example.org.key";  //Provide your client certificate .key.pem file path

var certificate = 
    new X509Certificate2(X509Certificate2.CreateFromPemFile(pemFile, keyFile)
        .Export(X509ContentType.Pkcs12));

var mqttClient = new MqttFactory().CreateMqttClient();

var connAck = await mqttClient!.ConnectAsync(new MqttClientOptionsBuilder()
    .WithTcpServer(hostname, 8883)
    .WithClientId(clientId)
    .WithCredentials("client1-authnID", "")  //use client authentication name in the username
    .WithTls(new MqttClientOptionsBuilderTlsParameters()
    {
        UseTls = true,
        Certificates = new X509Certificate2Collection(certificate)
    })
    .Build());

Console.WriteLine($"Client Connected: {mqttClient.IsConnected} with CONNACK: {connAck.ResultCode}");

mqttClient.ApplicationMessageReceivedAsync += 
    async m => await Console.Out.WriteAsync($"Received message on topic: '{m.ApplicationMessage.Topic}' with content: '{m.ApplicationMessage.ConvertPayloadToString()}'\n\n");

var suback = await mqttClient.SubscribeAsync("contosotopics/topic1");
suback.Items.ToList().ForEach(s => Console.WriteLine($"subscribed to '{s.TopicFilter.Topic}' with '{s.ResultCode}'"));

while (true)
{
    var puback = await mqttClient.PublishStringAsync("contosotopics/topic1", $"Hello world at {DateTimeOffset.UtcNow:o}!");
    Console.WriteLine(puback.ReasonString);
    await Task.Delay(5000);
}