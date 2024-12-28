package com.example.project1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.project1.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<com.example.project1.model.User, Long> {
    Optional<User> findByUsername(String username);
}
