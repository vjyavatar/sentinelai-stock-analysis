package com.sentinel.stockanalysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Authentication & User DTOs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SignupRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    private String fullName;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class AuthResponse {
    private String token;
    private String type = "Bearer";
    private UserDTO user;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UserDTO {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}

/**
 * Stock Analysis DTOs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class StockAnalysisRequest {
    @NotBlank(message = "Ticker is required")
    @Size(max = 20, message = "Ticker must be less than 20 characters")
    private String ticker;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class StockAnalysisResponse {
    private Long reportId;
    private String ticker;
    private String companyName;
    private BigDecimal currentPrice;
    private String currency;
    private String recommendation;
    private Map<String, Object> liveData;
    private String analysis;
    private LocalDateTime generatedAt;
    private Boolean success;
    private String shareUrl;
}

/**
 * Python Service DTOs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PythonAnalysisRequest {
    @JsonProperty("company_name")
    private String companyName;
    
    private String email;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class PythonAnalysisResponse {
    private Boolean success;
    
    @JsonProperty("live_data")
    private Map<String, Object> liveData;
    
    private String report;
    
    private String error;
    
    // Helper methods
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success);
    }
    
    public String getTicker() {
        if (liveData != null && liveData.containsKey("ticker")) {
            return (String) liveData.get("ticker");
        }
        return null;
    }
    
    public String getCompanyName() {
        if (liveData != null && liveData.containsKey("company_name")) {
            return (String) liveData.get("company_name");
        }
        return null;
    }
}

/**
 * Watchlist DTOs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WatchlistRequest {
    @NotBlank(message = "Ticker is required")
    private String ticker;
    
    private BigDecimal targetPrice;
    private BigDecimal stopLoss;
    private String notes;
    private Boolean alertEnabled;
    private BigDecimal alertPrice;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WatchlistResponse {
    private Long id;
    private String ticker;
    private String companyName;
    private BigDecimal addedPrice;
    private BigDecimal currentPrice;
    private BigDecimal targetPrice;
    private BigDecimal stopLoss;
    private BigDecimal changePercent;
    private String notes;
    private Boolean alertEnabled;
    private LocalDateTime addedAt;
}

/**
 * Portfolio DTOs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PortfolioRequest {
    @NotBlank(message = "Ticker is required")
    private String ticker;
    
    private BigDecimal shares;
    private BigDecimal buyPrice;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PortfolioResponse {
    private Long id;
    private String ticker;
    private String companyName;
    private BigDecimal shares;
    private BigDecimal averageBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPct;
    private LocalDateTime purchasedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PortfolioSummary {
    private BigDecimal totalValue;
    private BigDecimal totalInvested;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPct;
    private Integer totalHoldings;
    private java.util.List<PortfolioResponse> holdings;
}

/**
 * Dashboard DTOs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class DashboardResponse {
    private UserStats userStats;
    private java.util.List<StockReportSummary> recentReports;
    private java.util.List<WatchlistResponse> watchlist;
    private PortfolioSummary portfolioSummary;
    private java.util.List<TrendingStock> trendingStocks;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UserStats {
    private Long totalReports;
    private Long totalWatchlist;
    private Long totalPortfolio;
    private LocalDateTime memberSince;
    private LocalDateTime lastActive;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class StockReportSummary {
    private Long id;
    private String ticker;
    private String companyName;
    private String recommendation;
    private LocalDateTime generatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TrendingStock {
    private String ticker;
    private Long analysisCount;
}

/**
 * Error Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ErrorResponse {
    private String error;
    private String message;
    private Integer status;
    private LocalDateTime timestamp;
    private String path;
}
