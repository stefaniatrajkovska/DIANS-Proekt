package com.example.project1.web.controller;

import com.example.project1.dto.Response;
import com.example.project1.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/perform-technical-analysis")
    public ResponseEntity<String> performTechnicalAnalysis(@RequestParam(name = "corporationId") Long companyId) {
        if (companyId == null || companyId <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid company ID provided.");
        }

        try {
            String response = aiService.technicalAnalysis(companyId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to perform technical analysis: " + e.getMessage());
        }
    }

    @GetMapping("/sentiment-analysis")
    public ResponseEntity<Response> performSentimentAnalysis(@RequestParam(name = "corporationId") Long companyId) {
        if (companyId == null || companyId <= 0) {
            Response response = new Response();
            response.setRecommendation("Invalid company ID.");
            response.setSentimentScore(null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            Response response = aiService.nlp(companyId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Response errorResponse = new Response();
            errorResponse.setRecommendation("Error during sentiment analysis: " + e.getMessage());
            errorResponse.setSentimentScore(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/price-prediction")
    public ResponseEntity<Response> predictFuturePrice(@RequestParam(name = "corporationId") Long companyId) {
        if (companyId == null || companyId <= 0) {
            Response response = new Response();
            response.setRecommendation("Invalid company ID.");
            response.setSentimentScore(null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            Double predictedPrice = aiService.lstm(companyId);
            Response response = new Response();
            if (predictedPrice == null) {
                response.setRecommendation("Prediction not available.");
                response.setSentimentScore(null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            response.setRecommendation("Predicted Price: " + predictedPrice);
            response.setSentimentScore(null);  // Optionally set this to a specific value if required
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Response errorResponse = new Response();
            errorResponse.setRecommendation("Error during price prediction: " + e.getMessage());
            errorResponse.setSentimentScore(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
