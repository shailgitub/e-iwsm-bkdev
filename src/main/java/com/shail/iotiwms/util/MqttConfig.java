package com.shail.iotiwms.util;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig {
    private static final String MQTT_BROKER = "ssl://cf6085d108c5492aa2d3ada56f805db4.s1.eu.hivemq.cloud:8883";
    private static final String MQTT_CLIENT_ID = "SpringBootAPI-" + System.currentTimeMillis();
    private static final String MQTT_USERNAME = "swapisticated";
    private static final String MQTT_PASSWORD = "awmsMqtt1";

    @Bean
    public MqttClient mqttClient() throws MqttException {
        MqttClient client = new MqttClient(MQTT_BROKER, MQTT_CLIENT_ID, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(MQTT_USERNAME);
        options.setPassword(MQTT_PASSWORD.toCharArray());
        options.setConnectionTimeout(60);
        options.setKeepAliveInterval(60);
        client.connect(options);
        return client;
    }
}
