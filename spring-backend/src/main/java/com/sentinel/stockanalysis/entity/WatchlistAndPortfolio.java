package com.sentinel.stockanalysis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Watchlist Entity - Tracks stocks user wants to monitor
 */
@Entity
@Table(name = "watchlists", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ticker"}),
    indexes = {
        @Index(name = "idx_watchlist_user", columnList = "user_id"),
        @Index(name = "idx_watchlist_ticker", columnList = "ticker")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Watchlist {

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

    @Column(name = "added_price", precision = 12, scale = 2)
    private BigDecimal addedPrice;

    @Column(name = "target_price", precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Column(name = "stop_loss", precision = 12, scale = 2)
    private BigDecimal stopLoss;

    @Column(length = 500)
    private String notes;

    @Column(name = "alert_enabled")
    @Builder.Default
    private Boolean alertEnabled = false;

    @Column(name = "alert_price", precision = 12, scale = 2)
    private BigDecimal alertPrice;

    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Column(name = "last_checked")
    private LocalDateTime lastChecked;
}

/**
 * Portfolio Entity - Tracks actual stock holdings
 */
@Entity
@Table(name = "portfolios",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ticker"}),
    indexes = {
        @Index(name = "idx_portfolio_user", columnList = "user_id"),
        @Index(name = "idx_portfolio_ticker", columnList = "ticker")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Portfolio {

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

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal shares;

    @Column(name = "average_buy_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal averageBuyPrice;

    @Column(name = "current_price", precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "total_invested", precision = 12, scale = 2)
    private BigDecimal totalInvested;

    @Column(name = "current_value", precision = 12, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "profit_loss", precision = 12, scale = 2)
    private BigDecimal profitLoss;

    @Column(name = "profit_loss_pct", precision = 6, scale = 2)
    private BigDecimal profitLossPct;

    @CreatedDate
    @Column(name = "purchased_at", nullable = false, updatable = false)
    private LocalDateTime purchasedAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Helper methods
    public void updateCurrentValue(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        this.currentValue = currentPrice.multiply(shares);
        this.profitLoss = currentValue.subtract(totalInvested);
        this.profitLossPct = profitLoss
            .divide(totalInvested, 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"));
        this.lastUpdated = LocalDateTime.now();
    }

    public void addShares(BigDecimal additionalShares, BigDecimal buyPrice) {
        BigDecimal totalCost = totalInvested.add(buyPrice.multiply(additionalShares));
        BigDecimal totalShares = shares.add(additionalShares);
        this.averageBuyPrice = totalCost.divide(totalShares, 2, BigDecimal.ROUND_HALF_UP);
        this.shares = totalShares;
        this.totalInvested = totalCost;
        updateCurrentValue(this.currentPrice);
    }
}
