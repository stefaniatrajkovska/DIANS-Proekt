package com.example.project1.service;

import com.example.project1.model.CorporationIssuer;
import com.example.project1.model.CorporationData;
import com.example.project1.dto.Response;
import com.example.project1.repository.CorporationDataRepository;
import com.example.project1.repository.CorporationIssuerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final CorporationDataRepository corporationDataRepository;
    private final CorporationIssuerRepository corporationIssuerRepository;

    private final String TECHNICAL_ANALYSIS_URL = "http://127.0.0.1:5000/generate_signal";
    private final String NLP_URL = "http://127.0.0.1:5000/analyze";
    private final String LSTM_URL = "http://127.0.0.1:8000/predict-next-month-price/";

    public String technicalAnalysis(Long companyId) {
        List<CorporationData> companyData = getCompanyData(companyId);
        List<Map<String, Object>> payload = preparePayload(companyData);
        HttpEntity<List<Map<String, Object>>> requestEntity = createRequestEntity(payload);
        Map<String, Object> responseBody = sendRequestToPythonApi(requestEntity);

        return extractSignalFromResponse(responseBody);
    }

    private List<CorporationData> getCompanyData(Long companyId) {
        return corporationDataRepository.findByCorporationIssuerId(companyId);
    }

    private List<Map<String, Object>> preparePayload(List<CorporationData> data) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (CorporationData d : data) {
            Map<String, Object> record = new HashMap<>();
            record.put("date", d.getDate().toString());
            record.put("close", d.getTransaction_price());
            record.put("open", (d.getMax_price() + d.getMin_price()) / 2.0);
            record.put("high", d.getMax_price());
            record.put("low", d.getMin_price());
            record.put("volume", d.getQuantity());
            payload.add(record);
        }
        return payload;
    }

    private HttpEntity<List<Map<String, Object>>> createRequestEntity(List<Map<String, Object>> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(payload, headers);
    }

    private Map<String, Object> sendRequestToPythonApi(HttpEntity<List<Map<String, Object>>> requestEntity) {
        ResponseEntity<Map> responseEntity = restTemplate.exchange(
                TECHNICAL_ANALYSIS_URL,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );
        return responseEntity.getBody();
    }

    private String extractSignalFromResponse(Map<String, Object> responseBody) {
        if (responseBody != null && responseBody.containsKey("final_signal")) {
            return responseBody.get("final_signal").toString();
        } else {
            throw new RuntimeException("Failed to retrieve a valid signal from the Python API.");
        }
    }


    public Response nlp(Long companyId) throws Exception {
        CorporationIssuer company = getCompanyById(companyId);
        String companyCode = company.getCorporationCode();

        HttpEntity<String> requestEntity = createRequestEntity();
        ResponseEntity<Map> responseEntity = sendNlpRequest(companyCode, requestEntity);

        Map<String, Object> responseBody = getResponseBody(responseEntity);
        handleApiError(responseBody);

        return extractSentimentAndRecommendation(responseBody);
    }

    private CorporationIssuer getCompanyById(Long companyId) throws Exception {
        return corporationIssuerRepository.findById(companyId)
                .orElseThrow(() -> new Exception("Company not found"));
    }

    private HttpEntity<String> createRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    private ResponseEntity<Map> sendNlpRequest(String companyCode, HttpEntity<String> requestEntity) {
        String url = buildNlpUrl(companyCode);
        return restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);
    }

    private String buildNlpUrl(String companyCode) {
        return NLP_URL + "?company_code=" + companyCode;
    }

    private Map<String, Object> getResponseBody(ResponseEntity<Map> responseEntity) {
        return responseEntity.getBody();
    }

    private void handleApiError(Map<String, Object> responseBody) {
        if (responseBody != null && responseBody.containsKey("error")) {
            String errorMessage = (String) responseBody.get("error");
            throw new RuntimeException("Error from Python API: " + errorMessage);
        }
    }

    private Response extractSentimentAndRecommendation(Map<String, Object> responseBody) {
        if (responseBody == null) {
            throw new RuntimeException("Failed to retrieve sentiment analysis from the Python API.");
        }

        Response response = new Response();
        response.sentimentScore = (Double) responseBody.get("sentiment_score");
        response.recommendation = (String) responseBody.get("recommendation");
        return response;
    }


    public Double lstm(Long companyId) {
        List<CorporationData> recentData = getRecentCompanyData(companyId);
        Map<String, Object> requestBody = prepareRequestBody(recentData);

        HttpEntity<Map<String, Object>> requestEntity = createRequestEntity(requestBody);
        Map<String, Double> response = sendLstmRequest(requestEntity);

        return extractPredictedPrice(response);
    }

    private List<CorporationData> getRecentCompanyData(Long companyId) {
        return corporationDataRepository.findByCorporationIssuerIdAndDateBetween(
                companyId, LocalDate.now().minusMonths(3), LocalDate.now());
    }

    private Map<String, Object> prepareRequestBody(List<CorporationData> data) {
        return Map.of("data", mapToRequestData(data));
    }

    private HttpEntity<Map<String, Object>> createRequestEntity(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(requestBody, headers);
    }

    private Map<String, Double> sendLstmRequest(HttpEntity<Map<String, Object>> requestEntity) {
        return restTemplate.postForObject(LSTM_URL, requestEntity, Map.class);
    }

    private Double extractPredictedPrice(Map<String, Double> response) {
        return response != null ? response.get("predicted_next_month_price") : null;
    }


    public static List<Map<String, Object>> mapToRequestData(List<CorporationData> historicalDataEntities) {
        return historicalDataEntities.stream().map(entity -> {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("date", entity.getDate().toString());
            dataMap.put("average_price", entity.getAvg_price());
            return dataMap;
        }).collect(Collectors.toList());
    }

}
