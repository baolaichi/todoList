package com.example.inventory.repository;

import com.example.inventory.model.SystemReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemReviewRepository extends JpaRepository<SystemReview, Long> {
    List<SystemReview> findByIsApprovedTrue();
}
