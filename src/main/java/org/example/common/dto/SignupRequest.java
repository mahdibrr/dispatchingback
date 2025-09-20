package org.example.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email String email,
    @NotBlank @Size(max = 20) String phone,
    @NotBlank @Size(min = 6, max = 100) String password,
    // optional role (e.g. "DRIVER") — absent for web signups
    String role
) {}
