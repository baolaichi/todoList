package com.example.inventory.controller;

import com.example.inventory.model.SystemReview;
import com.example.inventory.repository.SystemReviewRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/reviews")
public class PublicReviewController {

    private final SystemReviewRepository systemReviewRepository;

    public PublicReviewController(SystemReviewRepository systemReviewRepository) {
        this.systemReviewRepository = systemReviewRepository;
    }

    @PostMapping
    public ResponseEntity<SystemReview> createReview(@RequestBody SystemReview review) {
        review.setApproved(false);
        return ResponseEntity.ok(systemReviewRepository.save(review));
    }

    @GetMapping
    public ResponseEntity<List<SystemReview>> getApprovedReviews() {
        return ResponseEntity.ok(systemReviewRepository.findByIsApprovedTrue());
    }
}
