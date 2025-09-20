package org.example.shared.repository;

import org.example.shared.entity.User;
import org.example.shared.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    List<User> findAllByRole(UserRole role);
    List<User> findByRoleOrderByNameAsc(UserRole role);
    List<User> findByRoleAndNameContainingIgnoreCaseOrderByNameAsc(UserRole role, String name);
    List<User> findByRoleAndPhoneContainingIgnoreCaseOrderByNameAsc(UserRole role, String phone);
}
