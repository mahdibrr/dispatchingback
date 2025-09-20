package org.example.driver.controller;

import org.example.common.dto.AddressDto;
import org.example.dispatcher.dto.MissionDto;
import org.example.common.dto.UserDto;
import org.example.shared.entity.Mission;
import org.example.shared.entity.User;
import org.example.common.service.MissionService;
import org.example.dispatcher.realtime.DispatcherRealtimeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/driver/missions")
public class DriverMissionController {

    private final MissionService missionService;
    private final DispatcherRealtimeService realtimeService;

    public DriverMissionController(MissionService missionService, DispatcherRealtimeService realtimeService) {
        this.missionService = missionService;
        this.realtimeService = realtimeService;
    }

    @GetMapping
    public List<MissionDto> listMyMissions(@AuthenticationPrincipal(expression = "user") User user) {
        return missionService.findByDriver(user.getId()).stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public MissionDto getMission(@AuthenticationPrincipal(expression = "user") User user,
                                 @PathVariable("id") UUID id) {
        // For drivers, we get missions they are assigned to
        List<Mission> driverMissions = missionService.findByDriver(user.getId());
        Mission mission = driverMissions.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Mission not found or not assigned to you"));
        return toDto(mission);
    }

    @PostMapping("/{id}/pickup")
    public MissionDto pickupMission(@AuthenticationPrincipal(expression = "user") User user,
                                    @PathVariable("id") UUID id) {
        System.out.println("DriverMissionController.pickupMission called: mission=" + id + " user=" + user.getId());
        Mission updated = missionService.markPickedUp(id, user.getId());
    // publish status change to mission channel + global status stream
    System.out.println("DriverMissionController.pickupMission publishing status=" + updated.getStatus());
    realtimeService.notifyMissionStatusChanged(
        updated.getId(),
        updated.getDriver() != null ? updated.getDriver().getId() : null,
        updated.getStatus().name(),
        updated.getAssignedAt() != null ? String.valueOf(updated.getAssignedAt().toEpochMilli()) : null,
        updated.getPickedUpAt() != null ? String.valueOf(updated.getPickedUpAt().toEpochMilli()) : null,
        updated.getInTransitAt() != null ? String.valueOf(updated.getInTransitAt().toEpochMilli()) : null,
        updated.getDeliveredAt() != null ? String.valueOf(updated.getDeliveredAt().toEpochMilli()) : null
    );
    return toDto(updated);
    }

    @PostMapping("/{id}/start-transit")
    public MissionDto startTransit(@AuthenticationPrincipal(expression = "user") User user,
                                   @PathVariable("id") UUID id) {
        System.out.println("DriverMissionController.startTransit called: mission=" + id + " user=" + user.getId());
        Mission updated = missionService.markInTransit(id, user.getId());
    System.out.println("DriverMissionController.startTransit publishing status=" + updated.getStatus());
    realtimeService.notifyMissionStatusChanged(
        updated.getId(),
        updated.getDriver() != null ? updated.getDriver().getId() : null,
        updated.getStatus().name(),
        updated.getAssignedAt() != null ? String.valueOf(updated.getAssignedAt().toEpochMilli()) : null,
        updated.getPickedUpAt() != null ? String.valueOf(updated.getPickedUpAt().toEpochMilli()) : null,
        updated.getInTransitAt() != null ? String.valueOf(updated.getInTransitAt().toEpochMilli()) : null,
        updated.getDeliveredAt() != null ? String.valueOf(updated.getDeliveredAt().toEpochMilli()) : null
    );
    return toDto(updated);
    }

    @PostMapping("/{id}/deliver")
    public MissionDto deliverMission(@AuthenticationPrincipal(expression = "user") User user,
                                     @PathVariable("id") UUID id) {
        System.out.println("DriverMissionController.deliverMission called: mission=" + id + " user=" + user.getId());
        Mission updated = missionService.markDelivered(id, user.getId());
    System.out.println("DriverMissionController.deliverMission publishing status=" + updated.getStatus());
    realtimeService.notifyMissionStatusChanged(
        updated.getId(),
        updated.getDriver() != null ? updated.getDriver().getId() : null,
        updated.getStatus().name(),
        updated.getAssignedAt() != null ? String.valueOf(updated.getAssignedAt().toEpochMilli()) : null,
        updated.getPickedUpAt() != null ? String.valueOf(updated.getPickedUpAt().toEpochMilli()) : null,
        updated.getInTransitAt() != null ? String.valueOf(updated.getInTransitAt().toEpochMilli()) : null,
        updated.getDeliveredAt() != null ? String.valueOf(updated.getDeliveredAt().toEpochMilli()) : null
    );
    return toDto(updated);
    }

    private MissionDto toDto(Mission mission) {
        AddressDto pickup = null;
        if (mission.getPickup() != null) {
            pickup = new AddressDto(
                    mission.getPickup().getLine1(),
                    mission.getPickup().getCity(),
                    mission.getPickup().getPostalCode(),
                    mission.getPickup().getNotes(),
                    mission.getPickup().getLat(),
                    mission.getPickup().getLng()
            );
        }

        AddressDto dropoff = null;
        if (mission.getDropoff() != null) {
            dropoff = new AddressDto(
                    mission.getDropoff().getLine1(),
                    mission.getDropoff().getCity(),
                    mission.getDropoff().getPostalCode(),
                    mission.getDropoff().getNotes(),
                    mission.getDropoff().getLat(),
                    mission.getDropoff().getLng()
            );
        }

        UserDto driverDto = null;
        if (mission.getDriver() != null) {
            driverDto = new UserDto(
                    mission.getDriver().getId(),
                    mission.getDriver().getName(),
                    mission.getDriver().getEmail(),
                    mission.getDriver().getPhone(),
                    mission.getDriver().getRole().name()
            );
        }

        return new MissionDto(
                mission.getId(),
                mission.getReference(),
                mission.getStatus().name(),
                pickup,
                dropoff,
                mission.getCreatedAt(),
                mission.getUpdatedAt(),
                mission.getAssignedAt(),
                mission.getPickedUpAt(),
                mission.getInTransitAt(),
                mission.getDeliveredAt(),
                mission.getEta() != null ? mission.getEta().toString() : null,
                mission.getPriceEstimate(),
                driverDto,
                mission.getParcelSize(),
                mission.getParcelNotes()
        );
    }
}