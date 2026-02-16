package com.sentinel.stockanalysis.service;

import com.sentinel.stockanalysis.dto.*;
import com.sentinel.stockanalysis.entity.StockReport;
import com.sentinel.stockanalysis.entity.User;
import com.sentinel.stockanalysis.repository.StockReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stock Analysis Service
 * 
 * Core business logic for stock analysis operations
 * 
 * Features:
 * - Orchestrates Python AI service calls
 * - Saves reports to PostgreSQL
 * - Implements caching for performance
 * - Handles rate limiting
 * - Manages report sharing
 * 
 * @author Sentinel AI Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockAnalysisService {

    private final PythonAIServiceClient pythonClient;
    private final StockReportRepository reportRepository;
    private final RateLimitService rateLimitService;

    /**
     * Generate complete stock analysis
     * 
     * Flow:
     * 1. Check user rate limits
     * 2. Call Python AI service
     * 3. Save report to database
     * 4. Return formatted response
     */
    @Transactional
    public StockAnalysisResponse generateAnalysis(String ticker, User user) {
        log.info("📊 Generating analysis for {} requested by {}", ticker, user.getEmail());

        // Check rate limits
        if (!rateLimitService.allowRequest(user)) {
            throw new RuntimeException("Rate limit exceeded. Please try again later.");
        }

        try {
            // Call Python service
            PythonAnalysisResponse pythonResponse = pythonClient.getAnalysis(
                    ticker,
                    user.getEmail()
            );

            if (!pythonResponse.isSuccessful()) {
                throw new RuntimeException(
                        pythonResponse.getError() != null ? 
                        pythonResponse.getError() : 
                        "Failed to generate analysis"
                );
            }

            // Save to database
            StockReport report = saveReport(user, ticker, pythonResponse);

            // Build response
            return buildResponse(report, pythonResponse);

        } catch (Exception e) {
            log.error("❌ Error generating analysis for {}: {}", ticker, e.getMessage());
            throw new RuntimeException("Analysis generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Save stock report to database
     */
    private StockReport saveReport(User user, String ticker, PythonAnalysisResponse pythonResponse) {
        Map<String, Object> liveData = pythonResponse.getLiveData();
        
        StockReport report = StockReport.builder()
                .user(user)
                .ticker(ticker.toUpperCase())
                .companyName(getStringFromMap(liveData, "company_name"))
                .currentPrice(getBigDecimalFromMap(liveData, "current_price"))
                .currency(getStringFromMap(liveData, "currency"))
                .marketCap(getLongFromMap(liveData, "market_cap"))
                .analysisText(pythonResponse.getReport())
                .liveData(Map.of(
                        "live_data", liveData,
                        "report", pythonResponse.getReport(),
                        "success", true
                ))
                .recommendation(extractRecommendation(pythonResponse.getReport()))
                .isShared(false)
                .build();

        StockReport saved = reportRepository.save(report);
        log.info("✅ Report saved with ID: {}", saved.getId());
        
        return saved;
    }

    /**
     * Build API response from saved report
     */
    private StockAnalysisResponse buildResponse(StockReport report, PythonAnalysisResponse pythonResponse) {
        return StockAnalysisResponse.builder()
                .reportId(report.getId())
                .ticker(report.getTicker())
                .companyName(report.getCompanyName())
                .currentPrice(report.getCurrentPrice())
                .currency(report.getCurrency())
                .recommendation(report.getRecommendation())
                .liveData(pythonResponse.getLiveData())
                .analysis(pythonResponse.getReport())
                .generatedAt(report.getCreatedAt())
                .success(true)
                .build();
    }

    /**
     * Get user's report history
     */
    @Transactional(readOnly = true)
    public Page<StockReportSummary> getUserReports(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StockReport> reports = reportRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        
        return reports.map(report -> StockReportSummary.builder()
                .id(report.getId())
                .ticker(report.getTicker())
                .companyName(report.getCompanyName())
                .recommendation(report.getRecommendation())
                .generatedAt(report.getCreatedAt())
                .build());
    }

    /**
     * Get specific report by ID
     */
    @Transactional(readOnly = true)
    public StockAnalysisResponse getReport(Long reportId, User user) {
        StockReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (!report.isOwnedBy(user) && !report.getIsShared()) {
            throw new RuntimeException("Access denied");
        }

        // Increment view count
        report.incrementViewCount();
        reportRepository.save(report);

        Map<String, Object> liveDataMap = report.getLiveDataMap();

        return StockAnalysisResponse.builder()
                .reportId(report.getId())
                .ticker(report.getTicker())
                .companyName(report.getCompanyName())
                .currentPrice(report.getCurrentPrice())
                .currency(report.getCurrency())
                .recommendation(report.getRecommendation())
                .liveData(liveDataMap)
                .analysis(report.getReport())
                .generatedAt(report.getCreatedAt())
                .success(true)
                .shareUrl(report.getIsShared() ? generateShareUrl(report.getShareToken()) : null)
                .build();
    }

    /**
     * Share a report (generate public link)
     */
    @Transactional
    public String shareReport(Long reportId, User user) {
        StockReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (!report.isOwnedBy(user)) {
            throw new RuntimeException("Access denied");
        }

        if (!report.getIsShared()) {
            report.setIsShared(true);
            report.setShareToken(UUID.randomUUID().toString());
            reportRepository.save(report);
        }

        return generateShareUrl(report.getShareToken());
    }

    /**
     * Get report by share token (public access)
     */
    @Transactional(readOnly = true)
    public StockAnalysisResponse getSharedReport(String shareToken) {
        StockReport report = reportRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new RuntimeException("Shared report not found"));

        if (!report.getIsShared()) {
            throw new RuntimeException("This report is not shared");
        }

        // Increment view count
        report.incrementViewCount();
        reportRepository.save(report);

        Map<String, Object> liveDataMap = report.getLiveDataMap();

        return StockAnalysisResponse.builder()
                .reportId(report.getId())
                .ticker(report.getTicker())
                .companyName(report.getCompanyName())
                .currentPrice(report.getCurrentPrice())
                .currency(report.getCurrency())
                .recommendation(report.getRecommendation())
                .liveData(liveDataMap)
                .analysis(report.getReport())
                .generatedAt(report.getCreatedAt())
                .success(true)
                .build();
    }

    /**
     * Search user reports
     */
    @Transactional(readOnly = true)
    public Page<StockReportSummary> searchReports(User user, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StockReport> reports = reportRepository.searchUserReports(user, query, pageable);
        
        return reports.map(report -> StockReportSummary.builder()
                .id(report.getId())
                .ticker(report.getTicker())
                .companyName(report.getCompanyName())
                .recommendation(report.getRecommendation())
                .generatedAt(report.getCreatedAt())
                .build());
    }

    /**
     * Get trending stocks (cached)
     */
    @Cacheable(value = "trendingStocks", unless = "#result.isEmpty()")
    @Transactional(readOnly = true)
    public List<TrendingStock> getTrendingStocks(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Pageable pageable = PageRequest.of(0, limit);
        
        List<Object[]> results = reportRepository.findTrendingStocks(since, pageable);
        
        return results.stream()
                .map(row -> TrendingStock.builder()
                        .ticker((String) row[0])
                        .analysisCount((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    // Helper methods
    private String extractRecommendation(String analysisText) {
        if (analysisText == null) return "HOLD";
        
        String upper = analysisText.toUpperCase();
        if (upper.contains("STRONG BUY") || (upper.contains("BUY") && !upper.contains("DON'T BUY"))) {
            return "BUY";
        } else if (upper.contains("SELL") || upper.contains("AVOID")) {
            return "SELL";
        }
        return "HOLD";
    }

    private String getStringFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private BigDecimal getBigDecimalFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Long getLongFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String generateShareUrl(String shareToken) {
        // In production, this would be your actual domain
        return "https://sentinel-ai.com/share/" + shareToken;
    }
}
