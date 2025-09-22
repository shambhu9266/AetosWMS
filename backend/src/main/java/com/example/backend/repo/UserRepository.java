package com.example.backend.repo;

import com.example.backend.model.User;
import com.example.backend.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(UserRole role);
    List<User> findByDepartment(String department);
    List<User> findByIsActiveTrue();
}
