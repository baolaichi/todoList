package com.example.inventory.repository;

import com.example.inventory.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("SELECT u FROM Users u WHERE u.email = :email")
    public Optional<Users> findByEmail(String email);

    public Users findByOtp(String otp);

}
