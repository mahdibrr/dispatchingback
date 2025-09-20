package org.example.driver.realtime;

import org.example.realtime.CentrifugoClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DriverRealtimeService {

    private final CentrifugoClient centrifugo;

    public DriverRealtimeService(CentrifugoClient centrifugo) {
        this.centrifugo = centrifugo;
    }

    // Notify a specific driver that a mission has been assigned (driver inbox)
    public void notifyDriverMissionAssignment(UUID driverId, UUID missionId) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "assignment");
        event.put("missionId", missionId.toString());
        event.put("status", "ASSIGNED");
        event.put("timestamp", Instant.now().toString());

        // driver-scoped channel for their inbox
        centrifugo.publish(centrifugo.driverChannel(driverId), event);

        // also mirror to mission-scoped + global status so dispatcher UI updates immediately
        centrifugo.publish(centrifugo.missionChannel(missionId), event);
        centrifugo.publish(centrifugo.statusChannel(), event);
    }

    public void notifyMissionPickup(UUID missionId, UUID driverId) {
        // Method removed - no longer publishing status updates
    }

    public void notifyMissionTransitStart(UUID missionId, UUID driverId) {
        // Method removed - no longer publishing status updates
    }

    public void notifyMissionDelivery(UUID missionId, UUID driverId) {
        // Method removed - no longer publishing status updates
    }
}