package com.example;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
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
        options.addOption("cs", "cleanSession", false, "Clean session");
        options.addOption("cc", "clientCertPath", true, "Client certificate path (PKCS12)");
        options.addOption("pw", "clientCertPassword", true, "Client certificate password");

        options.addOption("pub", "publish", false, "Publish message to topic");
        options.addOption("sub", "subscribe", false, "Subscribe to topic");

        String message = "Hello MQTT from Java!";
        boolean isPublisher = false;
        boolean isSubscriber = false;

        long pid = Thread.currentThread().getId();
		String defaultClientId = "mqtt-client-" + pid;

        MqttClientOptions clientOptions = new MqttClientOptions();
        clientOptions.setClientId(defaultClientId);
        clientOptions.setPassword("");
        clientOptions.setPort(8883);
        clientOptions.setCleanSession(false);

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
            if (cmd.hasOption("cs")) {
                clientOptions.setCleanSession(true);
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
            e.printStackTrace(System.err);
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
        final PrintStream outStream = System.out;

        try {
            String uri = String.format("ssl://%s:%d", clientOptions.getBroker(), clientOptions.getPort());

            MemoryPersistence persistence = new MemoryPersistence();
            MqttAsyncClient client = new MqttAsyncClient(uri, clientOptions.getClientId(), persistence);
            client.setCallback(new DefaultMqttCallback(outStream));
            clientWrapper.setClient(client);

            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setUserName(clientOptions.getUsername());
            options.setPassword(clientOptions.getPassword().getBytes(StandardCharsets.UTF_8));
            options.setSocketFactory(MutualTLSSocketFactory.create(clientOptions.getClientCertPath(),
                    clientOptions.getClientCertPassword()));
            options.setCleanStart(clientOptions.isCleanSession());

            outStream.println(MessageFormat.format(
                "Connecting to broker {0} as user {1} with client ID {2} [clean session {3}]", 
                uri, 
                clientOptions.getUsername(), 
                clientOptions.getClientId(),
                clientOptions.isCleanSession()));
            IMqttToken connectToken = client.connect(options);
            connectToken.waitForCompletion();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MqttAsyncClient wrappedClient = clientWrapper.getClient();
                if (wrappedClient != null && wrappedClient.isConnected()) {
                    try {
                        outStream.println("Disconnecting from broker due to shutdown signal...");
                        isRunning = false;
                        IMqttToken disconnectToken = wrappedClient.disconnect();
                        disconnectToken.waitForCompletion();
                        outStream.println("Disconnected from broker.");
                    } catch (MqttException e) {
                        e.printStackTrace(System.err);
                    } finally {
                        latch.countDown();
                    }
                }
            }));

            final String topic = clientOptions.getTopic();

            if (isSubscriber) {
                IMqttToken subscriptionToken = client.subscribe(topic, 1);
                System.out.println("Subscribed to topic: " + topic);
                subscriptionToken.waitForCompletion();
                System.out.println("Subscription complete.");
            } else if (isPublisher) {
                System.out.println("Publishing to topic: " + topic);
                new Thread(() -> {
                    try {
                        for (int i = 1; isRunning; i++) {
                            String payload = String.format("%s #%d", message, i);
                            MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                            mqttMessage.setQos(1);
                            IMqttToken deliveryToken = client.publish(topic, mqttMessage);
                            deliveryToken.waitForCompletion();
                            Thread.sleep(2000);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }).start();
            }

            // Block execution until a Signal is received
            latch.await();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        } finally {
            MqttAsyncClient client = clientWrapper.getClient();
            if (client != null) {
                try {
                    client.close();
                } catch (MqttException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

}
