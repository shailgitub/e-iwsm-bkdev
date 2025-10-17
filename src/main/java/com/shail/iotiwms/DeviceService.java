package com.shail.iotiwms;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;


import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceService {
    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Object>> telemetryData = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> valveStatus = new ConcurrentHashMap<>();

    public DeviceService(MqttClient mqttClient, ObjectMapper objectMapper) {
        this.mqttClient = mqttClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws MqttException {
        mqttClient.subscribe("water/level", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                JsonNode jsonNode = objectMapper.readTree(payload);
                String deviceId = jsonNode.get("deviceId").asText();
                telemetryData.put(deviceId, objectMapper.convertValue(jsonNode, Map.class));
                System.out.println("Received telemetry: " + payload);
            }
        });

        mqttClient.subscribe("water/valve", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                JsonNode jsonNode = objectMapper.readTree(payload);
                String deviceId = jsonNode.get("deviceId").asText();
                valveStatus.put(deviceId, objectMapper.convertValue(jsonNode, Map.class));
                System.out.println("Received valve status: " + payload);
            }
        });
    }

    @PreDestroy
    public void destroy() throws MqttException {
        mqttClient.disconnect();
        mqttClient.close();
    }

    public void sendCommand(String deviceId, String command, int value) throws MqttException, JsonProcessingException {
        Map<String, Object> commandPayload = new HashMap<>();
        commandPayload.put("deviceId", deviceId);
        commandPayload.put("command", command);
        commandPayload.put("value", value);
        String payload = objectMapper.writeValueAsString(commandPayload);
        mqttClient.publish("water/commands", new MqttMessage(payload.getBytes()));
        System.out.println("Sent command: " + payload);
    }

    public Map<String, Object> getTelemetry(String deviceId) {
        return telemetryData.getOrDefault(deviceId, new HashMap<>());
    }

    public Map<String, Object> getValveStatus(String deviceId) {
        return valveStatus.getOrDefault(deviceId, new HashMap<>());
    }
}