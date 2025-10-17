package com.shail.iotiwms;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/device")
public class IwmsDeviceController {
    private final DeviceService deviceService;

    public IwmsDeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/command")
    public ResponseEntity<String> sendCommand(@RequestBody Map<String, Object> request) {
        try {
            String deviceId = (String) request.get("deviceId");
            String command = (String) request.get("command");
            int value = request.containsKey("value") ? ((Number) request.get("value")).intValue() : 0;
            deviceService.sendCommand(deviceId, command, value);
            return ResponseEntity.ok("Command sent successfully");
        } catch (MqttException | JsonProcessingException e) {
            return ResponseEntity.status(500).body("Failed to send command: " + e.getMessage());
        }
    }

    @GetMapping("/telemetry/{deviceId}")
    public ResponseEntity<Map<String, Object>> getTelemetry(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceService.getTelemetry(deviceId));
    }

    @GetMapping("/valve/{deviceId}")
    public ResponseEntity<Map<String, Object>> getValveStatus(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceService.getValveStatus(deviceId));
    }
}
