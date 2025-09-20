package org.example.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
    
public record ContactDto(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 20) String phone
) {}
