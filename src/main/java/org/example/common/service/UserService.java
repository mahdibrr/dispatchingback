package org.example.common.service;

import org.example.shared.entity.User;
import org.example.shared.entity.UserRole;
import org.example.shared.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@Service
public class UserService {
    public java.util.List<User> searchDrivers(String q) {
        if (q == null || q.isBlank()) {
            return userRepository.findByRoleOrderByNameAsc(UserRole.DRIVER);
        }
        java.util.List<User> byName = userRepository.findByRoleAndNameContainingIgnoreCaseOrderByNameAsc(UserRole.DRIVER, q);
        if (!byName.isEmpty()) return byName;
        return userRepository.findByRoleAndPhoneContainingIgnoreCaseOrderByNameAsc(UserRole.DRIVER, q);
    }
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User updateProfile(UUID userId, String name, String phone) {
        return userRepository.findById(userId).map(u -> {
            u.setName(name);
            u.setPhone(phone == null ? "" : phone);
            return userRepository.save(u);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
