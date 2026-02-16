package com.sentinel.stockanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sentinel AI Stock Analysis Platform
 * 
 * A comprehensive stock analysis platform powered by:
 * - Spring Boot for robust backend services
 * - PostgreSQL for data persistence
 * - Redis for high-performance caching
 * - Python AI service for intelligent analysis
 * 
 * Features:
 * - User authentication & authorization
 * - Real-time stock data analysis
 * - AI-powered investment recommendations
 * - Portfolio tracking & management
 * - Report history & sharing
 * - Watchlist management
 * 
 * @author Sentinel AI Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class StockAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockAnalysisApplication.class, args);
        System.out.println("""
            
            ╔══════════════════════════════════════════════════════════════╗
            ║                                                              ║
            ║        ⚡ SENTINEL AI STOCK ANALYSIS PLATFORM ⚡            ║
            ║                                                              ║
            ║  Status: ✅ RUNNING                                         ║
            ║  Port: 8080                                                  ║
            ║  Environment: Production-Ready                               ║
            ║                                                              ║
            ║  Features:                                                   ║
            ║  • AI-Powered Stock Analysis                                 ║
            ║  • Real-Time Market Data                                     ║
            ║  • User Portfolio Tracking                                   ║
            ║  • Smart Caching & Rate Limiting                             ║
            ║                                                              ║
            ╚══════════════════════════════════════════════════════════════╝
            
            """);
    }
}
