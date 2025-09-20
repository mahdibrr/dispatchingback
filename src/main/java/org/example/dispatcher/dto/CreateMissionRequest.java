package org.example.dispatcher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.example.common.dto.AddressDto;
import org.example.common.dto.ContactDto;

public record CreateMissionRequest(
    @NotNull @Valid AddressDto pickup,
    @NotNull @Valid AddressDto dropoff,
    @NotNull @Valid ContactDto contactPickup,
    @NotNull @Valid ContactDto contactDropoff,
    @NotNull String packageSize,
    String notes
) {}
