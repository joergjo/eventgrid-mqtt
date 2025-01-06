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

public class EventGridMqttSample {

    public static void main(String[] args) {
        String broker = null;
        String clientId = MqttClient.generateClientId();
        String username = null;
        String topic = null;
        String message = null;
        String clientCertPath = null;
        String clientCertPassword = null;
        Integer port = 8883;

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

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("b")) {
                broker = cmd.getOptionValue("b");
            }
            if (cmd.hasOption("p")) {
                port = (Integer) cmd.getParsedOptionValue("p");
            }
            if (cmd.hasOption("id")) {
                clientId = cmd.getOptionValue("id");
            }
            if (cmd.hasOption("u")) {
                username = cmd.getOptionValue("u");
            }
            if (cmd.hasOption("t")) {
                topic = cmd.getOptionValue("t");
            }
            if (cmd.hasOption("m")) {
                message = cmd.getOptionValue("m");
            }
            if (cmd.hasOption("cc")) {
                clientCertPath = cmd.getOptionValue("cc");
            }
            if (cmd.hasOption("pw")) {
                clientCertPassword = cmd.getOptionValue("pw");
            }
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        pubSub(broker, port.intValue(), clientId, username, topic, message, clientCertPath, clientCertPassword);
    }

    private static void pubSub(String broker, int port, String clientId, String username, String topic, String message,
            String clientCertPath, String clientCertPassword) {
        if (broker == null || clientId == null || username == null || topic == null || message == null
                || clientCertPath == null || clientCertPassword == null) {
            System.err.println("Missing required arguments");
            System.exit(1);
        }
        MqttClient client = null;
        try {
            String uri = String.format("ssl://%s:%d", broker, port);
            client = new MqttClient(uri, clientId);

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
                        System.out.println(MessageFormat.format("Callback: delivered message to topics {0}",
                                Arrays.asList(token.getTopics())));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            // We don't use a password 
            options.setPassword("".toCharArray());
            options.setSocketFactory(MutualTLSSocketFactory.create(clientCertPath, clientCertPassword));

            System.out.println("Connecting to broker: " + uri);
            client.connect(options);

            if (!client.isConnected()) {
                System.err.println("Failed to connect to broker: " + uri);
                return;
            }
            System.out.println("Connected to broker: " + uri);

            client.subscribe(topic, 1);
            System.out.println("Subscribed to topic: " + topic);

            MqttMessage msg = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
            msg.setQos(1);
            client.publish(topic, msg);

            System.out.println("Disconnect from broker: " + uri);
            client.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
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
