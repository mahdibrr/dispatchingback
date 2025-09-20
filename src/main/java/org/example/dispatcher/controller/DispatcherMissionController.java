package org.example.dispatcher.controller;

import jakarta.validation.Valid;
import org.example.common.dto.AddressDto;
import org.example.dispatcher.dto.AssignMissionRequest;
import org.example.dispatcher.dto.CreateMissionRequest;
import org.example.dispatcher.dto.MissionDto;
import org.example.common.dto.UserDto;
import org.example.shared.entity.Mission;
import org.example.shared.entity.User;
import org.example.common.service.MissionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dispatcher/missions")
public class DispatcherMissionController {

    private final MissionService missionService;

    public DispatcherMissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping
    public List<MissionDto> listMissions(@AuthenticationPrincipal(expression = "user") User user) {
        return missionService.findByOwner(user.getId()).stream().map(this::toDto).collect(Collectors.toList());
    }

    @PostMapping
    public MissionDto createMission(@AuthenticationPrincipal(expression = "user") User user,
                                    @Valid @RequestBody CreateMissionRequest req) {
        Mission mission = missionService.createForOwner(user.getId(), req);
        return toDto(mission);
    }

    @GetMapping("/{id}")
    public MissionDto getMission(@AuthenticationPrincipal(expression = "user") User user,
                                 @PathVariable("id") UUID id) {
        Mission mission = missionService.getOwned(user.getId(), id);
        return toDto(mission);
    }

    @PostMapping("/{id}/cancel")
    public MissionDto cancelMission(@AuthenticationPrincipal(expression = "user") User user,
                                    @PathVariable("id") UUID id) {
        // Endpoint removed - no longer allowing status updates
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED, "Status updates not allowed");
    }

    @PostMapping("/{id}/assign")
    public MissionDto assign(@PathVariable("id") UUID missionId,
                             @Valid @RequestBody AssignMissionRequest req) {
        Mission updated = missionService.assignDriver(missionId, req.driverId());
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
