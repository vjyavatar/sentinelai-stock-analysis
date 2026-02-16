package com.sentinel.stockanalysis.repository;

import com.sentinel.stockanalysis.entity.StockReport;
import com.sentinel.stockanalysis.entity.User;
import com.sentinel.stockanalysis.entity.Watchlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Repository - Data access for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Optional<User> findByVerificationToken(String token);
    
    Optional<User> findByResetToken(String token);
    
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.isVerified = true")
    List<User> findAllActiveUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countNewUsersince(@Param("since") LocalDateTime since);
}

/**
 * StockReport Repository - Data access for StockReport entity
 */
@Repository
public interface StockReportRepository extends JpaRepository<StockReport, Long> {
    
    Page<StockReport> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    List<StockReport> findByUserAndTickerOrderByCreatedAtDesc(User user, String ticker);
    
    Optional<StockReport> findByShareToken(String shareToken);
    
    @Query("SELECT sr FROM StockReport sr WHERE sr.user = :user " +
           "AND sr.createdAt >= :since ORDER BY sr.createdAt DESC")
    List<StockReport> findRecentReports(
        @Param("user") User user,
        @Param("since") LocalDateTime since
    );
    
    @Query("SELECT sr.ticker, COUNT(sr) as count FROM StockReport sr " +
           "WHERE sr.createdAt >= :since GROUP BY sr.ticker " +
           "ORDER BY count DESC")
    List<Object[]> findTrendingStocks(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT sr FROM StockReport sr WHERE sr.user = :user " +
           "AND LOWER(sr.ticker) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(sr.companyName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY sr.createdAt DESC")
    Page<StockReport> searchUserReports(
        @Param("user") User user,
        @Param("search") String search,
        Pageable pageable
    );
    
    long countByUser(User user);
    
    @Query("SELECT COUNT(sr) FROM StockReport sr WHERE sr.createdAt >= :since")
    long countReportsGenerated since(@Param("since") LocalDateTime since);
    
    // JSONB queries for advanced filtering
    @Query(value = "SELECT * FROM stock_reports WHERE user_id = :userId " +
                   "AND live_data->>'recommendation' = :recommendation " +
                   "ORDER BY created_at DESC",
           nativeQuery = true)
    List<StockReport> findByRecommendation(
        @Param("userId") Long userId,
        @Param("recommendation") String recommendation
    );
}

/**
 * Watchlist Repository - Data access for Watchlist entity
 */
@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    
    List<Watchlist> findByUserOrderByAddedAtDesc(User user);
    
    Optional<Watchlist> findByUserAndTicker(User user, String ticker);
    
    boolean existsByUserAndTicker(User user, String ticker);
    
    void deleteByUserAndTicker(User user, String ticker);
    
    long countByUser(User user);
    
    @Query("SELECT w FROM Watchlist w WHERE w.user = :user " +
           "AND w.alertEnabled = true AND w.alertPrice IS NOT NULL")
    List<Watchlist> findActiveAlerts(@Param("user") User user);
}

/**
 * Portfolio Repository - Data access for Portfolio entity
 */
@Repository
public interface PortfolioRepository extends JpaRepository<com.sentinel.stockanalysis.entity.Portfolio, Long> {
    
    List<com.sentinel.stockanalysis.entity.Portfolio> findByUserOrderByPurchasedAtDesc(User user);
    
    Optional<com.sentinel.stockanalysis.entity.Portfolio> findByUserAndTicker(User user, String ticker);
    
    boolean existsByUserAndTicker(User user, String ticker);
    
    @Query("SELECT SUM(p.currentValue) FROM Portfolio p WHERE p.user = :user")
    Optional<java.math.BigDecimal> calculateTotalPortfolioValue(@Param("user") User user);
    
    @Query("SELECT SUM(p.profitLoss) FROM Portfolio p WHERE p.user = :user")
    Optional<java.math.BigDecimal> calculateTotalProfitLoss(@Param("user") User user);
    
    long countByUser(User user);
}
