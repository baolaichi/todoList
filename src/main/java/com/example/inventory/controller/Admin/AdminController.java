package com.example.inventory.controller.Admin;

import com.example.inventory.model.Users;
import com.example.inventory.model.dto.AdminDashboardDTO;
import com.example.inventory.model.SystemReview;
import com.example.inventory.service.AdminDashboardService;
import com.example.inventory.repository.SystemReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private com.example.inventory.repository.SurveyRepository surveyRepository;

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardDTO> getStats(){
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }

    @Autowired
    private SystemReviewRepository systemReviewRepository;

    @GetMapping("/reviews")
    public ResponseEntity<List<SystemReview>> getAllReviews() {
        return ResponseEntity.ok(systemReviewRepository.findAll());
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        systemReviewRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reviews/{id}/approve")
    public ResponseEntity<Void> approveReview(@PathVariable Long id) {
        SystemReview review = systemReviewRepository.findById(id).orElseThrow();
        review.setApproved(true);
        systemReviewRepository.save(review);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<Users>> getAllUsers() {
        return ResponseEntity.ok(adminDashboardService.getAllUsers());
    }

    @GetMapping("/surveys")
    public ResponseEntity<List<com.example.inventory.model.Survey>> getAllSurveys() {
        return ResponseEntity.ok(surveyRepository.findAll());
    }

    @DeleteMapping("/surveys/{id}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long id) {
        surveyRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
