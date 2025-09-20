package org.example.common.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
    @NotBlank String name,
    String phone
) {}
