package org.example.common.controller;

import org.springframework.web.bind.annotation.RequestParam;
import org.example.common.dto.UserDto;
import org.example.common.dto.UpdateProfileRequest;
import org.example.shared.entity.User;
import org.example.common.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/users")
    public java.util.List<UserDto> listDrivers(
        @RequestParam(name = "role", defaultValue = "DRIVER") String role,
        @RequestParam(name = "q", required = false) String q
    ) {
        java.util.List<User> drivers = users.searchDrivers(q);
        return drivers.stream()
            .map(u -> new UserDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole().name()))
            .toList();
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal(expression = "user") User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name());
    }

    @PutMapping("/me")
    public UserDto updateMe(@AuthenticationPrincipal(expression = "user") User user, @RequestBody UpdateProfileRequest req) {
        User updated = users.updateProfile(user.getId(), req.name(), req.phone());
        return new UserDto(updated.getId(), updated.getName(), updated.getEmail(), updated.getPhone(), updated.getRole().name());
    }
}
