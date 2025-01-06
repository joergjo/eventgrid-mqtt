package com.example;

import org.eclipse.paho.client.mqttv3.MqttClient;

public class MqttClientWrapper {

    private MqttClient client;

    public MqttClient getClient() {
        return client;
    }

    public void setClient(MqttClient client) {
        this.client = client;
    }

}
