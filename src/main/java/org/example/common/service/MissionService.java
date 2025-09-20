// src/main/java/org/example/service/MissionService.java
package org.example.common.service;

import org.example.dispatcher.dto.CreateMissionRequest;
import org.example.shared.entity.Address;
import org.example.shared.entity.Mission;
import org.example.shared.entity.MissionStatus;
import org.example.shared.entity.User;
import org.example.shared.entity.UserRole;
import org.example.shared.repository.MissionRepository;
import org.example.shared.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import org.example.dispatcher.realtime.DispatcherRealtimeService;

@Service
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final DispatcherRealtimeService dispatcherRealtimeService;

    public MissionService(
            MissionRepository missionRepository,
            UserRepository userRepository,
            DispatcherRealtimeService dispatcherRealtimeService
    ) {
        this.missionRepository = missionRepository;
        this.userRepository = userRepository;
        this.dispatcherRealtimeService = dispatcherRealtimeService;
    }

    /* ======================= Query ======================= */

    public List<Mission> findByOwner(UUID ownerId) {
        User owner = userRepository.findById(ownerId).orElseThrow();
        return missionRepository.findByOwner(owner);
    }

    public List<Mission> findByDriver(UUID driverId) {
        User driver = userRepository.findById(driverId).orElseThrow();
        return missionRepository.findByDriver(driver);
    }

    public Mission getOwned(UUID ownerId, UUID missionId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found"));

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission not found"));

        if (mission.getOwner() == null || !mission.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        return mission;
    }

    /* ======================= Commands ======================= */

    @Transactional
    public Mission createForOwner(UUID ownerId, CreateMissionRequest req) {
        User owner = userRepository.findById(ownerId).orElseThrow();

        Mission mission = new Mission();
        mission.setReference(generateReference());
        mission.setStatus(MissionStatus.PENDING);

        Address pickup = new Address();
        pickup.setLine1(req.pickup().line1());
        pickup.setCity(req.pickup().city());
        pickup.setPostalCode(req.pickup().postalCode());
        pickup.setNotes(req.pickup().notes());
        pickup.setLat(req.pickup().lat());
        pickup.setLng(req.pickup().lng());

        Address dropoff = new Address();
        dropoff.setLine1(req.dropoff().line1());
        dropoff.setCity(req.dropoff().city());
        dropoff.setPostalCode(req.dropoff().postalCode());
        dropoff.setNotes(req.dropoff().notes());
        dropoff.setLat(req.dropoff().lat());
        dropoff.setLng(req.dropoff().lng());

        mission.setPickup(pickup);
        mission.setDropoff(dropoff);
        mission.setParcelSize(req.packageSize());
        mission.setParcelNotes(req.notes());

        mission.setOwner(owner);
        mission.setCreatedAt(Instant.now());
        mission.setUpdatedAt(null);
        mission.setPriceEstimate(null);

        int attempts = 0;
        while (true) {
            try {
                Mission saved = missionRepository.save(mission);

                return saved;
            } catch (DataIntegrityViolationException ex) {
                attempts++;
                if (attempts >= 5) throw ex;
                mission.setReference(generateReference());
            }
        }
    }

    @Transactional
    public Mission assignDriver(UUID missionId, String driverId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found"));

        UUID driverUuid;
        try {
            driverUuid = UUID.fromString(driverId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid driverId format");
        }

        User driver = userRepository.findById(driverUuid)
                .orElseThrow(() -> new IllegalArgumentException("User (driver) not found"));

        if (driver.getRole() != UserRole.DRIVER) {
            throw new IllegalArgumentException("Selected user is not a DRIVER");
        }

        mission.setDriver(driver);
        mission.setStatus(MissionStatus.ASSIGNED);
        mission.setAssignedAt(Instant.now());
        Mission saved = missionRepository.save(mission);

        // Notify realtime layer about the assignment so mobile/web clients receive updates
        try {
            UUID drvId = saved.getDriver() != null ? saved.getDriver().getId() : null;
            String drvName = saved.getDriver() != null ? saved.getDriver().getName() : null;
            // Use overload that includes mission summary so clients can render directly
            dispatcherRealtimeService.notifyMissionAssigned(saved, drvId, drvName);
        } catch (Exception e) {
            // don't fail the API call if realtime notification fails
            try { System.err.println("[MISSION SERVICE] failed to notify realtime: " + e.getMessage()); } catch (Exception ex) {}
        }

        return saved;
    }

    @Transactional
    public Mission cancelOwned(UUID ownerId, UUID missionId) {
        Mission mission = getOwned(ownerId, missionId);

        if (mission.getStatus() == MissionStatus.DELIVERED || mission.getStatus() == MissionStatus.CANCELLED) {
            throw new RuntimeException("Mission is already terminal");
        }

        mission.setStatus(MissionStatus.CANCELLED);
        mission.setUpdatedAt(Instant.now());
        Mission saved = missionRepository.save(mission);

        return saved;
    }

    @Transactional
    public Mission markPickedUp(UUID missionId, UUID driverId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission not found"));

        if (mission.getDriver() == null || !mission.getDriver().getId().equals(driverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mission not assigned to you");
        }

        if (mission.getStatus() == MissionStatus.DELIVERED || mission.getStatus() == MissionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mission is already terminal");
        }

        if (mission.getStatus() != MissionStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition");
        }

        mission.setStatus(MissionStatus.PICKED_UP);
        mission.setPickedUpAt(Instant.now());
        mission.setUpdatedAt(Instant.now());
        return missionRepository.save(mission);
    }

    @Transactional
    public Mission markInTransit(UUID missionId, UUID driverId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission not found"));

        if (mission.getDriver() == null || !mission.getDriver().getId().equals(driverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mission not assigned to you");
        }

        if (mission.getStatus() == MissionStatus.DELIVERED || mission.getStatus() == MissionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mission is already terminal");
        }

        if (mission.getStatus() != MissionStatus.PICKED_UP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition");
        }

        mission.setStatus(MissionStatus.IN_TRANSIT);
        mission.setInTransitAt(Instant.now());
        mission.setUpdatedAt(Instant.now());
        return missionRepository.save(mission);
    }

    @Transactional
    public Mission markDelivered(UUID missionId, UUID driverId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission not found"));

        if (mission.getDriver() == null || !mission.getDriver().getId().equals(driverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mission not assigned to you");
        }

        if (mission.getStatus() == MissionStatus.DELIVERED || mission.getStatus() == MissionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mission is already terminal");
        }

        if (mission.getStatus() != MissionStatus.IN_TRANSIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition");
        }

        mission.setStatus(MissionStatus.DELIVERED);
        mission.setDeliveredAt(Instant.now());
        mission.setUpdatedAt(Instant.now());
        return missionRepository.save(mission);
    }


    /* ======================= Helpers ======================= */

    private String generateReference() {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "MIS-" + date + "-" + suffix;
    }

}
