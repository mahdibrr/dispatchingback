// src/main/java/org/example/dispatcher/realtime/DispatcherRealtimeService.java
package org.example.dispatcher.realtime;

import org.example.realtime.CentrifugoClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DispatcherRealtimeService {

    private final CentrifugoClient centrifugo;

    public DispatcherRealtimeService(CentrifugoClient centrifugo) {
        this.centrifugo = centrifugo;
    }

    /** Assignment event + status=ASSIGNED on mission channel. Also mirrors to global status. */
    public void notifyMissionAssigned(java.util.UUID missionId, java.util.UUID driverId, String driverName) {
        // If caller supplies a mission object, use overloaded method. This signature remains
        // for compatibility; we attempt to publish a minimal summary if possible by loading
        // mission details via the mission service if available in the runtime.
        Map<String, Object> assignment = new HashMap<>();
        assignment.put("type", "assignment");
        assignment.put("missionId", missionId.toString());
        if (driverId != null) assignment.put("driverId", driverId.toString());
        if (driverName != null) assignment.put("driverName", driverName);
        assignment.put("status", "ASSIGNED");
        assignment.put("at", String.valueOf(System.currentTimeMillis()));

            // Log assignment to console for visibility
            try {
                System.out.println("[ASSIGNMENT] mission=" + missionId + " driver=" + driverId + " payload=" + assignment);
            } catch (Exception e) {
                // ignore logging errors
            }

        

        // Publish to mission-specific channel
        String missionChannel = centrifugo.missionChannel(missionId);
        centrifugo.publish(missionChannel, assignment);
        try {
            System.out.println("[ASSIGNMENT] published to mission channel: " + missionChannel);
        } catch (Exception e) {}

        // Also notify the assigned driver directly on their driver-scoped mission channel
        try {
            if (driverId != null) {
                // Mobile clients subscribe to `missions:<driverId>` so publish there
                String driverMissionChannel = centrifugo.missionChannel(driverId);
                centrifugo.publish(driverMissionChannel, assignment);
                try {
                    System.out.println("[ASSIGNMENT] published to driver channel: " + driverMissionChannel);
                } catch (Exception e) {}
            }
        } catch (Exception e) {
            // Log/ignore to avoid breaking assignment flow if driver publish fails
            try { System.err.println("[ASSIGNMENT] failed to publish to driver channel: " + e.getMessage()); } catch (Exception ex) {}
        }

        centrifugo.publishGlobalStatus(missionId, driverId, "ASSIGNED");
    }

    /**
     * Publish an assignment including mission summary fields so clients can render immediately
     * without an extra REST fetch.
     */
    public void notifyMissionAssigned(org.example.shared.entity.Mission mission, java.util.UUID driverId, String driverName) {
        if (mission == null) return;
        Map<String, Object> assignment = new HashMap<>();
        assignment.put("type", "assignment");
        assignment.put("missionId", mission.getId().toString());
        if (driverId != null) assignment.put("driverId", driverId.toString());
        if (driverName != null) assignment.put("driverName", driverName);
        assignment.put("status", "ASSIGNED");
        assignment.put("at", String.valueOf(System.currentTimeMillis()));

        // Mission summary: reference and pickup/dropoff brief info
        try {
            if (mission.getReference() != null) assignment.put("reference", mission.getReference());
            if (mission.getPickup() != null) {
                Map<String, Object> pu = new HashMap<>();
                if (mission.getPickup().getLine1() != null) pu.put("line1", mission.getPickup().getLine1());
                if (mission.getPickup().getCity() != null) pu.put("city", mission.getPickup().getCity());
                assignment.put("pickup", pu);
            }
            if (mission.getDropoff() != null) {
                Map<String, Object> doff = new HashMap<>();
                if (mission.getDropoff().getLine1() != null) doff.put("line1", mission.getDropoff().getLine1());
                if (mission.getDropoff().getCity() != null) doff.put("city", mission.getDropoff().getCity());
                assignment.put("dropoff", doff);
            }
        } catch (Exception ignored) {}

        // Publish to mission channel and driver-specific channel
        String missionChannel = centrifugo.missionChannel(mission.getId());
        centrifugo.publish(missionChannel, assignment);
        try { System.out.println("[ASSIGNMENT] published to mission channel: " + missionChannel); } catch (Exception e) {}

        try {
            if (driverId != null) {
                String driverMissionChannel = centrifugo.missionChannel(driverId);
                centrifugo.publish(driverMissionChannel, assignment);
                try { System.out.println("[ASSIGNMENT] published to driver channel: " + driverMissionChannel); } catch (Exception e) {}
            }
        } catch (Exception e) {
            try { System.err.println("[ASSIGNMENT] failed to publish to driver channel: " + e.getMessage()); } catch (Exception ex) {}
        }

        centrifugo.publishGlobalStatus(mission.getId(), driverId, "ASSIGNED");
    }

    /** Cancellation normalized as type=status with status=CANCELLED. Mirrors to global status. */
    public void notifyMissionCancelled(UUID missionId, String reason) {
        Map<String, Object> ev = new HashMap<>();
        ev.put("type", "status");
        ev.put("missionId", missionId.toString());
        ev.put("status", "CANCELLED");
        if (reason != null) ev.put("reason", reason);
        ev.put("at", String.valueOf(System.currentTimeMillis()));

        centrifugo.publish(centrifugo.missionChannel(missionId), ev);
        centrifugo.publishGlobalStatus(missionId, null, "CANCELLED");
    }

    /** Unified status change publisher used for PICKED_UP, IN_TRANSIT, DELIVERED, etc. */
    public void notifyMissionStatusChanged(UUID missionId, UUID driverId, String newStatus,
                                           String assignedAt, String pickedUpAt,
                                           String inTransitAt, String deliveredAt) {
        centrifugo.publishMissionStatus(
                missionId,
                driverId,
                newStatus,
                assignedAt,
                pickedUpAt,
                inTransitAt,
                deliveredAt
        );
        centrifugo.publishGlobalStatus(missionId, driverId, newStatus);
    }
}
