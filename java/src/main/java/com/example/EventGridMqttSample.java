package com.example;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class EventGridMqttSample {

    private static volatile boolean isRunning = true;

    public static void main(String[] args) {
        Options options = new Options();

        Option portOption = Option.builder("p")
                .longOpt("port")
                .hasArg()
                .desc("Port number")
                .type(Integer.class)
                .build();

        options.addOption("b", "broker", true, "Broker URL");
        options.addOption(portOption);
        options.addOption("id", "clientId", true, "Client ID");
        options.addOption("u", "username", true, "Username");
        options.addOption("t", "topic", true, "Topic");
        options.addOption("m", "message", true, "Message");
        options.addOption("cc", "clientCertPath", true, "Client certificate path (PKCS12)");
        options.addOption("pw", "clientCertPassword", true, "Client certificate password");

        options.addOption("pub", "publish", false, "Publish message to topic");
        options.addOption("sub", "subscribe", false, "Subscribe to topic");

        String message = "Hello MQTT from Java!";
        boolean isPublisher = false;
        boolean isSubscriber = false;

        MqttClientOptions clientOptions = new MqttClientOptions();
        clientOptions.setClientId(MqttClient.generateClientId());
        clientOptions.setPassword("");
        clientOptions.setPort(8883);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("b")) {
                clientOptions.setBroker(cmd.getOptionValue("b"));
            }
            if (cmd.hasOption("p")) {
                Integer port = (Integer) cmd.getParsedOptionValue("p");
                clientOptions.setPort(port.intValue());
            }
            if (cmd.hasOption("id")) {
                clientOptions.setClientId(cmd.getOptionValue("id"));
            }
            if (cmd.hasOption("u")) {
                clientOptions.setUsername(cmd.getOptionValue("u"));
            }
            if (cmd.hasOption("t")) {
                clientOptions.setTopic(cmd.getOptionValue("t"));
            }
            if (cmd.hasOption("cc")) {
                clientOptions.setClientCertPath(cmd.getOptionValue("cc"));
            }
            if (cmd.hasOption("pw")) {
                clientOptions.setClientCertPassword(cmd.getOptionValue("pw"));
            }
            if (cmd.hasOption("m")) {
                message = cmd.getOptionValue("m");
            }
            if (cmd.hasOption("pub")) {
                isPublisher = true;
            }
            if (cmd.hasOption("sub")) {
                isSubscriber = true;
            }
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        if (!clientOptions.validate() || (!isPublisher && !isSubscriber)) {
            System.err.println("Missing required arguments");
            System.exit(1);
        }

        run(clientOptions, message, isPublisher, isSubscriber);
    }

    private static void run(final MqttClientOptions clientOptions, final String message, final boolean isPublisher,
            final boolean isSubscriber) {
        final MqttClientWrapper clientWrapper = new MqttClientWrapper();
        final CountDownLatch latch = new CountDownLatch(1);

        try {
            String uri = String.format("ssl://%s:%d", clientOptions.getBroker(), clientOptions.getPort());
            MqttClient client = new MqttClient(uri, clientOptions.getClientId());
            clientWrapper.setClient(client);
            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println(MessageFormat.format("Connection lost. Cause: {0}", cause));
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println(MessageFormat.format("Callback: received message from topic {0}: {1}",
                            topic, message.toString()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    try {
                        System.out.println(MessageFormat.format("Callback: published message to topics {0}",
                                Arrays.asList(token.getTopics())));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(clientOptions.getUsername());
            options.setPassword(clientOptions.getPassword().toCharArray());
            options.setSocketFactory(MutualTLSSocketFactory.create(clientOptions.getClientCertPath(),
                    clientOptions.getClientCertPassword()));

            System.out.println("Connecting to broker: " + uri);
            client.connect(options);

            if (!client.isConnected()) {
                System.err.println("Failed to connect to broker: " + uri);
                return;
            }
            System.out.println("Connected to broker: " + uri);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MqttClient wrappedClient = clientWrapper.getClient();
                if (wrappedClient != null && wrappedClient.isConnected()) {
                    try {
                        System.out.println("Disconnecting from broker due to shutdown signal...");
                        isRunning = false;
                        wrappedClient.disconnect();
                        System.out.println("Disconnected from broker.");
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            }));

            final String topic = clientOptions.getTopic();

            if (isSubscriber) {
                client.subscribe(topic, 1);
                System.out.println("Subscribed to topic: " + topic);
            }

            if (isPublisher) {
                System.out.println("Publishing to topic: " + topic);
                new Thread(() -> {
                    try {
                        while (isRunning) {
                            MqttMessage msg = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
                            msg.setQos(1);
                            client.publish(topic, msg);
                            Thread.sleep(2000);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            // Block execution until a Signal is received
            latch.await();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            MqttClient client = clientWrapper.getClient();
            if (client != null) {
                try {
                    client.close();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
