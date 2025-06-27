package com.example;

import java.io.PrintStream;
import java.text.MessageFormat;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

public class DefaultMqttCallback implements MqttCallback {

    private final PrintStream stream;

    public DefaultMqttCallback(PrintStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("PrintStream cannot be null");
        }
        this.stream = stream;
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        String cause = null;
        if (disconnectResponse.getException().getMessage() != null) {
            cause = disconnectResponse.getException().getMessage();
        } else {
            cause = disconnectResponse.getReasonString();
        }
        stream.println(MessageFormat.format("The connection to the server was lost, cause: {0}.", cause));
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        stream.println(MessageFormat.format("An MQTT error occurred: {0}.", exception.getMessage()));
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        stream.println(MessageFormat.format("Received message from topic {0}: {1}", topic, message.toString()));
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        stream.println(MessageFormat.format("Message {0} was delivered.", token.getMessageId()));
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        stream.println(MessageFormat.format("Connection complete to server {0}, reconnect: {1}", serverURI, reconnect));
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        stream.println(MessageFormat.format("[UNSUPPORTD] Auth packet arrived with reason code {0} and method {1}",
                reasonCode, properties.getAuthenticationMethod()));
    }

}
