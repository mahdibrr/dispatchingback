package org.example.shared.repository;

import org.example.shared.entity.Mission;
import org.example.shared.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<Mission, UUID> {
    List<Mission> findByOwner(User owner);
    List<Mission> findByDriver(User driver);
}
