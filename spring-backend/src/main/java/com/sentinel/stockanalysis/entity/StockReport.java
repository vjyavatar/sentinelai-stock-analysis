package com.sentinel.stockanalysis.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * StockReport Entity - Stores AI-generated stock analysis reports
 * 
 * Uses JSONB for flexible storage of Python service responses
 * Allows querying within JSON fields for advanced analytics
 */
@Entity
@Table(name = "stock_reports", indexes = {
    @Index(name = "idx_user_ticker", columnList = "user_id, ticker"),
    @Index(name = "idx_ticker", columnList = "ticker"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_user_created", columnList = "user_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "current_price", precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(length = 10)
    private String currency;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "analysis_text", columnDefinition = "TEXT")
    private String analysisText;

    /**
     * Stores complete Python service response as JSONB
     * Contains: live_data, report, success flag, etc.
     * 
     * Example structure:
     * {
     *   "live_data": {
     *     "ticker": "AAPL",
     *     "current_price": 175.50,
     *     "pe_ratio": 28.5,
     *     ...
     *   },
     *   "report": "AI-generated analysis text...",
     *   "success": true
     * }
     */
    @Type(JsonBinaryType.class)
    @Column(name = "live_data", columnDefinition = "jsonb")
    private Map<String, Object> liveData;

    @Column(name = "recommendation", length = 10)
    private String recommendation; // BUY, SELL, HOLD

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "is_shared")
    @Builder.Default
    private Boolean isShared = false;

    @Column(name = "share_token", length = 64, unique = true)
    private String shareToken;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Helper methods
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isOwnedBy(User user) {
        return this.user != null && this.user.getId().equals(user.getId());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getLiveDataMap() {
        if (liveData != null && liveData.containsKey("live_data")) {
            return (Map<String, Object>) liveData.get("live_data");
        }
        return liveData;
    }

    public String getReport() {
        if (liveData != null && liveData.containsKey("report")) {
            return (String) liveData.get("report");
        }
        return analysisText;
    }
}
