package com.example.inventory.controller;

import com.example.inventory.model.Response;
import com.example.inventory.model.Survey;
import com.example.inventory.repository.ResponseRepository;
import com.example.inventory.repository.SurveyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/surveys")
public class PublicSurveyController {

    private final SurveyRepository surveyRepository;
    private final ResponseRepository responseRepository;

    public PublicSurveyController(SurveyRepository surveyRepository, ResponseRepository responseRepository) {
        this.surveyRepository = surveyRepository;
        this.responseRepository = responseRepository;
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Survey> getSurvey(@PathVariable Long id) {
        return surveyRepository.findById(id)
                .map(survey -> {
                    // Force initialize lazy collections before returning
                    survey.getQuestions().size(); 
                    survey.getQuestions().forEach(q -> {
                        if (q.getOptions() != null) {
                            q.getOptions().size();
                        }
                    });
                    return ResponseEntity.ok(survey);
                })
                .orElse(ResponseEntity.notFound().build());
    }

}
