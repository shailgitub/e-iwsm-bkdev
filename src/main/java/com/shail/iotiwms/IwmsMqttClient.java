package com.shail.iotiwms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IwmsMqttClient implements MqttCallback {
    private static final String MQTT_BROKER = "ssl://cf6085d108c5492aa2d3ada56f805db4.s1.eu.hivemq.cloud:8883";
    private static final String MQTT_CLIENT_ID = "JavaIwmsClient-" + System.currentTimeMillis();
    private static final String MQTT_USERNAME = "swapisticated";
    private static final String MQTT_PASSWORD = "awmsMqtt1";
    private static final String TOPIC_COMMANDS = "water/commands";
    private static final String TOPIC_FLOW_DATA = "water/level";
    private static final String TOPIC_VALVE_STATUS = "water/valve";

    private final MqttClient client;
    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Object>> telemetryData;
    private final Map<String, Map<String, Object>> valveStatus;

    public IwmsMqttClient() throws MqttException {
        this.objectMapper = new ObjectMapper();
        this.telemetryData = new ConcurrentHashMap<>();
        this.valveStatus = new ConcurrentHashMap<>();
        this.client = new MqttClient(MQTT_BROKER, MQTT_CLIENT_ID, new MemoryPersistence());
        this.client.setCallback(this);
        connect();
        subscribe();
    }

    private void connect() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(MQTT_USERNAME);
        options.setPassword(MQTT_PASSWORD.toCharArray());
        options.setConnectionTimeout(60);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);
        System.out.println("Connecting to MQTT broker...");
        client.connect(options);
        System.out.println("Connected to MQTT broker");
    }

    private void subscribe() throws MqttException {
        client.subscribe(TOPIC_FLOW_DATA, 1, (topic, message) -> {
            String payload = new String(message.getPayload());
            ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(payload);
            String deviceId = jsonNode.get("deviceId").asText();
            telemetryData.put(deviceId, objectMapper.convertValue(jsonNode, Map.class));
            System.out.println("Received telemetry on " + topic + ": " + payload);
        });

        client.subscribe(TOPIC_VALVE_STATUS, 1, (topic, message) -> {
            String payload = new String(message.getPayload());
            ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(payload);
            String deviceId = jsonNode.get("deviceId").asText();
            valveStatus.put(deviceId, objectMapper.convertValue(jsonNode, Map.class));
            System.out.println("Received valve status on " + topic + ": " + payload);
        });
        System.out.println("Subscribed to topics: " + TOPIC_FLOW_DATA + ", " + TOPIC_VALVE_STATUS);
    }

    public void sendCommand(String deviceId, String command, int value) throws MqttException, JsonProcessingException {
        ObjectNode commandPayload = objectMapper.createObjectNode();
        commandPayload.put("deviceId", deviceId);
        commandPayload.put("command", command);
        commandPayload.put("value", value);
        String payload = objectMapper.writeValueAsString(commandPayload);
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(1);
        client.publish(TOPIC_COMMANDS, message);
        System.out.println("Sent command: " + payload);
    }

    public Map<String, Object> getTelemetry(String deviceId) {
        return telemetryData.getOrDefault(deviceId, Map.of());
    }

    public Map<String, Object> getValveStatus(String deviceId) {
        return valveStatus.getOrDefault(deviceId, Map.of());
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("Connection lost: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Handled by subscribe() method
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("Message delivery complete: " + token.getMessageId());
    }

    public void disconnect() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
            System.out.println("Disconnected from MQTT broker");
        }
        client.close();
    }

    public static void main(String[] args) {
        try {
            IwmsMqttClient client = new IwmsMqttClient();

            // Example: Send a command
            client.sendCommand("esp1", "OPEN_VALVE", 0);
            Thread.sleep(7000); // Wait to observe telemetry

            client.sendCommand("esp1", "set_valve_position", 2000); // 50% open
            Thread.sleep(7000); // Wait to observe telemetry
            client.sendCommand("esp1", "CLOSE_VALVE", 0);
            Thread.sleep(7000); // Wait to observe telemetry

            // Retrieve and print telemetry
            System.out.println("Telemetry: " + client.getTelemetry("esp1"));
            System.out.println("Valve Status: " + client.getValveStatus("esp1"));

            // Keep the application running to receive messages
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    client.disconnect();
                } catch (MqttException e) {
                    System.err.println("Error disconnecting: " + e.getMessage());
                }
            }));

            // Keep the program running
            while (true) {
                Thread.sleep(1000);
            }
        } catch (MqttException | InterruptedException | JsonProcessingException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
