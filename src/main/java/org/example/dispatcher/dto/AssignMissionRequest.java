package org.example.dispatcher.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignMissionRequest(
    @NotBlank String driverId
) {}
