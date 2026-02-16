package com.sentinel.stockanalysis.service;

import com.sentinel.stockanalysis.dto.PythonAnalysisRequest;
import com.sentinel.stockanalysis.dto.PythonAnalysisResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Python AI Service Client
 * 
 * Handles communication with the Python FastAPI service
 * that performs stock data fetching and AI analysis
 * 
 * Features:
 * - Circuit breaker pattern for fault tolerance
 * - Automatic retry with exponential backoff
 * - Fallback responses for service failures
 * - Request/Response logging
 * 
 * @author Sentinel AI Team
 */
@Service
@Slf4j
public class PythonAIServiceClient {

    private final RestTemplate restTemplate;
    private final String pythonServiceUrl;

    public PythonAIServiceClient(
            RestTemplate restTemplate,
            @Value("${app.python.service.url}") String pythonServiceUrl) {
        this.restTemplate = restTemplate;
        this.pythonServiceUrl = pythonServiceUrl;
        log.info("✅ Python AI Service Client initialized. URL: {}", pythonServiceUrl);
    }

    /**
     * Generate stock analysis by calling Python service
     * 
     * @param ticker Stock ticker symbol (e.g., "AAPL", "RELIANCE.NS")
     * @param email User email for tracking
     * @return Python analysis response with live data and AI report
     */
    @CircuitBreaker(name = "pythonService", fallbackMethod = "getAnalysisFallback")
    @Retry(name = "pythonService")
    public PythonAnalysisResponse getAnalysis(String ticker, String email) {
        log.info("🤖 Calling Python AI service for ticker: {}", ticker);
        
        try {
            // Build request
            PythonAnalysisRequest request = PythonAnalysisRequest.builder()
                    .companyName(ticker)
                    .email(email)
                    .build();

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PythonAnalysisRequest> entity = new HttpEntity<>(request, headers);

            // Call Python service
            String endpoint = pythonServiceUrl + "/api/generate-report";
            log.debug("📡 POST {}", endpoint);

            ResponseEntity<PythonAnalysisResponse> response = restTemplate.postForEntity(
                    endpoint,
                    entity,
                    PythonAnalysisResponse.class
            );

            PythonAnalysisResponse body = response.getBody();
            
            if (body != null && body.isSuccessful()) {
                log.info("✅ Successfully received analysis for {}", ticker);
                return body;
            } else {
                log.warn("⚠️ Python service returned unsuccessful response for {}", ticker);
                throw new RuntimeException("Python service returned unsuccessful response");
            }

        } catch (Exception e) {
            log.error("❌ Error calling Python service for {}: {}", ticker, e.getMessage());
            throw new RuntimeException("Failed to generate analysis: " + e.getMessage(), e);
        }
    }

    /**
     * Verify Python service health
     */
    public boolean isServiceHealthy() {
        try {
            String healthEndpoint = pythonServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(
                    healthEndpoint,
                    String.class
            );
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("⚠️ Python service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fallback method when Python service is unavailable
     */
    private PythonAnalysisResponse getAnalysisFallback(String ticker, String email, Exception ex) {
        log.error("🔴 Circuit breaker activated for Python service. Using fallback for {}", ticker);
        
        PythonAnalysisResponse fallback = new PythonAnalysisResponse();
        fallback.setSuccess(false);
        fallback.setError("Stock analysis service is temporarily unavailable. Please try again in a few minutes.");
        
        // Create minimal live data
        Map<String, Object> liveData = new HashMap<>();
        liveData.put("ticker", ticker);
        liveData.put("error", "Service temporarily unavailable");
        fallback.setLiveData(liveData);
        
        fallback.setReport(String.format(
                "⚠️ Stock Analysis Temporarily Unavailable\n\n" +
                "We're experiencing high demand or technical issues with our analysis service.\n\n" +
                "What you can do:\n" +
                "• Try again in 2-3 minutes\n" +
                "• Check if ticker '%s' is valid on Yahoo Finance\n" +
                "• Contact support if the issue persists\n\n" +
                "Error details: %s", 
                ticker, 
                ex.getMessage()
        ));
        
        return fallback;
    }

    /**
     * Get quick stock price without full analysis (cached)
     */
    @CircuitBreaker(name = "pythonService")
    @Retry(name = "pythonService")
    public Map<String, Object> getQuickPrice(String ticker) {
        try {
            String endpoint = pythonServiceUrl + "/api/verify-price/" + ticker;
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    endpoint,
                    Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting quick price for {}: {}", ticker, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unable to fetch price");
            return errorResponse;
        }
    }
}
