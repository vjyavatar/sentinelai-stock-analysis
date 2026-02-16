package com.sentinel.stockanalysis.controller;

import com.sentinel.stockanalysis.dto.*;
import com.sentinel.stockanalysis.entity.User;
import com.sentinel.stockanalysis.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Stock Analysis Controller
 * 
 * REST API endpoints for stock analysis operations
 * 
 * Endpoints:
 * - POST /api/stocks/analyze - Generate new analysis
 * - GET /api/stocks/reports - Get user's reports
 * - GET /api/stocks/reports/{id} - Get specific report
 * - POST /api/stocks/reports/{id}/share - Share a report
 * - GET /api/stocks/trending - Get trending stocks
 * - GET /api/stocks/search - Search reports
 * 
 * @author Sentinel AI Team
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class StockAnalysisController {

    private final StockAnalysisService analysisService;
    private final RateLimitService rateLimitService;

    /**
     * Generate stock analysis
     * 
     * POST /api/stocks/analyze
     * Body: { "ticker": "AAPL" }
     */
    @PostMapping("/analyze")
    public ResponseEntity<StockAnalysisResponse> analyzeStock(
            @Valid @RequestBody StockAnalysisRequest request,
            @AuthenticationPrincipal User user) {
        
        log.info("📊 Analysis request for {} from {}", request.getTicker(), user.getEmail());

        try {
            StockAnalysisResponse response = analysisService.generateAnalysis(
                    request.getTicker(),
                    user
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Analysis failed: {}", e.getMessage());
            throw new RuntimeException("Analysis generation failed: " + e.getMessage());
        }
    }

    /**
     * Get user's report history
     * 
     * GET /api/stocks/reports?page=0&size=10
     */
    @GetMapping("/reports")
    public ResponseEntity<Page<StockReportSummary>> getUserReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        
        Page<StockReportSummary> reports = analysisService.getUserReports(user, page, size);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get specific report
     * 
     * GET /api/stocks/reports/{id}
     */
    @GetMapping("/reports/{id}")
    public ResponseEntity<StockAnalysisResponse> getReport(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        
        StockAnalysisResponse report = analysisService.getReport(id, user);
        return ResponseEntity.ok(report);
    }

    /**
     * Share a report (generate public link)
     * 
     * POST /api/stocks/reports/{id}/share
     */
    @PostMapping("/reports/{id}/share")
    public ResponseEntity<Map<String, String>> shareReport(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        
        String shareUrl = analysisService.shareReport(id, user);
        return ResponseEntity.ok(Map.of("shareUrl", shareUrl));
    }

    /**
     * Get shared report (public endpoint, no auth required)
     * 
     * GET /api/stocks/shared/{shareToken}
     */
    @GetMapping("/shared/{shareToken}")
    public ResponseEntity<StockAnalysisResponse> getSharedReport(
            @PathVariable String shareToken) {
        
        StockAnalysisResponse report = analysisService.getSharedReport(shareToken);
        return ResponseEntity.ok(report);
    }

    /**
     * Search user's reports
     * 
     * GET /api/stocks/search?q=AAPL&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<Page<StockReportSummary>> searchReports(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        
        Page<StockReportSummary> results = analysisService.searchReports(user, q, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Get trending stocks
     * 
     * GET /api/stocks/trending?limit=10
     */
    @GetMapping("/trending")
    public ResponseEntity<List<TrendingStock>> getTrendingStocks(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<TrendingStock> trending = analysisService.getTrendingStocks(limit);
        return ResponseEntity.ok(trending);
    }

    /**
     * Get user's rate limit status
     * 
     * GET /api/stocks/rate-limit
     */
    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(
            @AuthenticationPrincipal User user) {
        
        long remaining = rateLimitService.getRemainingRequests(user);
        int limit = user.isPremium() ? 100 : 10;
        
        return ResponseEntity.ok(Map.of(
                "remaining", remaining,
                "limit", limit,
                "tier", user.isPremium() ? "PREMIUM" : "FREE"
        ));
    }

    /**
     * Health check endpoint
     * 
     * GET /api/stocks/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "stock-analysis",
                "version", "1.0.0"
        ));
    }
}
