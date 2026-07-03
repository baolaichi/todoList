package com.example.inventory.controller;

import com.example.inventory.model.Survey;
import com.example.inventory.model.Users;
import com.example.inventory.model.Response;
import com.example.inventory.repository.SurveyRepository;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.repository.ResponseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final ResponseRepository responseRepository;

    public SurveyController(SurveyRepository surveyRepository, UserRepository userRepository, ResponseRepository responseRepository) {
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
        this.responseRepository = responseRepository;
    }

    private Users getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<Survey> createSurvey(@RequestBody Survey survey) {
        survey.setUser(getCurrentUser());
        if (survey.getQuestions() != null) {
            survey.getQuestions().forEach(q -> {
                q.setSurvey(survey);
                if (q.getOptions() != null) {
                    q.getOptions().forEach(o -> o.setQuestion(q));
                }
            });
        }
        return ResponseEntity.ok(surveyRepository.save(survey));
    }

    @GetMapping
    public ResponseEntity<List<Survey>> getMySurveys() {
        return ResponseEntity.ok(surveyRepository.findByUserId(getCurrentUser().getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Survey> getSurvey(@PathVariable Long id) {
        Survey survey = surveyRepository.findById(id).orElseThrow();
        if (survey.getUser().getId() != getCurrentUser().getId()) {
            throw new RuntimeException("Access denied");
        }
        return ResponseEntity.ok(survey);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Survey> updateSurvey(@PathVariable Long id, @RequestBody Survey updatedSurvey) {
        Survey survey = surveyRepository.findById(id).orElseThrow();
        if (survey.getUser().getId() != getCurrentUser().getId()) {
            throw new RuntimeException("Access denied");
        }
        
        survey.setTitle(updatedSurvey.getTitle());
        survey.setDescription(updatedSurvey.getDescription());
        
        survey.getQuestions().clear();
        if (updatedSurvey.getQuestions() != null) {
            updatedSurvey.getQuestions().forEach(q -> {
                q.setSurvey(survey);
                if (q.getOptions() != null) {
                    q.getOptions().forEach(o -> o.setQuestion(q));
                }
                survey.getQuestions().add(q);
            });
        }
        return ResponseEntity.ok(surveyRepository.save(survey));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long id) {
        Survey survey = surveyRepository.findById(id).orElseThrow();
        if (survey.getUser().getId() != getCurrentUser().getId()) {
            throw new RuntimeException("Access denied");
        }
        surveyRepository.delete(survey);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/responses")
    public ResponseEntity<Response> submitResponse(@PathVariable Long id, @RequestBody Response response) {
        Survey survey = surveyRepository.findById(id).orElseThrow();
        response.setSurvey(survey);
        response.setUser(getCurrentUser());
        
        int score = 0;
        int gradedQuestions = 0;

        if (response.getAnswers() != null) {
            for (com.example.inventory.model.Answer a : response.getAnswers()) {
                a.setResponse(response);
                if (a.getQuestion() != null && a.getQuestion().getId() != null) {
                    com.example.inventory.model.Question q = survey.getQuestions().stream()
                        .filter(quest -> quest.getId().equals(a.getQuestion().getId()))
                        .findFirst().orElse(null);
                    
                    if (q != null) {
                        a.setQuestion(q);
                        if ("SINGLE_CHOICE".equals(q.getType()) || "MULTIPLE_CHOICE".equals(q.getType())) {
                            gradedQuestions++;
                            boolean isCorrect = q.getOptions().stream()
                                .anyMatch(opt -> Boolean.TRUE.equals(opt.getIsCorrect()) && opt.getContent().equals(a.getContent()));
                            if (isCorrect) {
                                score++;
                            }
                        } else if ("TEXT".equals(q.getType()) && q.getCorrectAnswer() != null && !q.getCorrectAnswer().trim().isEmpty()) {
                            gradedQuestions++;
                            // Case-insensitive comparison, ignore leading/trailing whitespace
                            if (a.getContent() != null && a.getContent().trim().equalsIgnoreCase(q.getCorrectAnswer().trim())) {
                                score++;
                            }
                        }
                    }
                }
            }
        }
        response.setScore(score);
        response.setTotalGradedQuestions(gradedQuestions);

        return ResponseEntity.ok(responseRepository.save(response));
    }

    @GetMapping("/{id}/responses")
    public ResponseEntity<List<Response>> getSurveyResponses(@PathVariable Long id) {
        Survey survey = surveyRepository.findById(id).orElseThrow();
        if (survey.getUser().getId() != getCurrentUser().getId()) {
            throw new RuntimeException("Access denied");
        }
        return ResponseEntity.ok(responseRepository.findBySurveyId(id));
    }
}
