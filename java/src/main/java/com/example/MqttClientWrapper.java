package com.example;

import org.eclipse.paho.mqttv5.client.MqttAsyncClient;;

public class MqttClientWrapper {

    private MqttAsyncClient client;

    public MqttAsyncClient getClient() {
        return client;
    }

    public void setClient(MqttAsyncClient client) {
        this.client = client;
    }

}
