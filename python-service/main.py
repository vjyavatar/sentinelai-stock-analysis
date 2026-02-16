"""
Sentinel AI Stock Analysis - Python FastAPI Service
Complete autonomous data collection and AI analysis agent
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, EmailStr
import yfinance as yf
import anthropic
import os
from datetime import datetime
from typing import Optional, Dict, Any
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastAPI
app = FastAPI(
    title="Sentinel AI Stock Analysis",
    description="Autonomous stock analysis agent powered by AI",
    version="1.0.0"
)

# CORS Configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize Anthropic client
ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY")
if not ANTHROPIC_API_KEY:
    logger.warning("ANTHROPIC_API_KEY not set - AI features will be limited")

anthropic_client = anthropic.Anthropic(api_key=ANTHROPIC_API_KEY) if ANTHROPIC_API_KEY else None


# Request/Response Models
class AnalysisRequest(BaseModel):
    company_name: str
    email: EmailStr


class AnalysisResponse(BaseModel):
    success: bool
    live_data: Optional[Dict[str, Any]] = None
    report: Optional[str] = None
    error: Optional[str] = None


# Agent: Data Collection
def fetch_stock_data(ticker: str) -> Dict[str, Any]:
    """
    Autonomous Data Collection Agent
    
    Capabilities:
    - Validates ticker
    - Fetches real-time market data
    - Calculates financial metrics
    - Handles errors independently
    """
    try:
        logger.info(f"🤖 Data Agent: Fetching data for {ticker}")
        
        # Download stock data
        stock = yf.Ticker(ticker)
        info = stock.info
        hist = stock.history(period="1y")
        
        if hist.empty:
            raise ValueError(f"No data found for ticker: {ticker}")
        
        # Calculate metrics
        current_price = hist['Close'].iloc[-1]
        year_high = hist['High'].max()
        year_low = hist['Low'].min()
        avg_volume = hist['Volume'].mean()
        
        # Price changes
        price_change_1d = ((hist['Close'].iloc[-1] - hist['Close'].iloc[-2]) / hist['Close'].iloc[-2] * 100) if len(hist) > 1 else 0
        price_change_1m = ((hist['Close'].iloc[-1] - hist['Close'].iloc[-21]) / hist['Close'].iloc[-21] * 100) if len(hist) > 21 else 0
        
        # Compile data
        live_data = {
            "ticker": ticker.upper(),
            "company_name": info.get("longName", ticker),
            "sector": info.get("sector", "Unknown"),
            "industry": info.get("industry", "Unknown"),
            "current_price": round(current_price, 2),
            "currency": info.get("currency", "USD"),
            "market_cap": info.get("marketCap", 0),
            "pe_ratio": round(info.get("trailingPE", 0), 2) if info.get("trailingPE") else None,
            "forward_pe": round(info.get("forwardPE", 0), 2) if info.get("forwardPE") else None,
            "peg_ratio": round(info.get("pegRatio", 0), 2) if info.get("pegRatio") else None,
            "price_to_book": round(info.get("priceToBook", 0), 2) if info.get("priceToBook") else None,
            "profit_margin": round(info.get("profitMargins", 0) * 100, 2) if info.get("profitMargins") else None,
            "operating_margin": round(info.get("operatingMargins", 0) * 100, 2) if info.get("operatingMargins") else None,
            "roe": round(info.get("returnOnEquity", 0) * 100, 2) if info.get("returnOnEquity") else None,
            "debt_to_equity": round(info.get("debtToEquity", 0), 2) if info.get("debtToEquity") else None,
            "revenue_growth": round(info.get("revenueGrowth", 0) * 100, 2) if info.get("revenueGrowth") else None,
            "earnings_growth": round(info.get("earningsGrowth", 0) * 100, 2) if info.get("earningsGrowth") else None,
            "52_week_high": round(year_high, 2),
            "52_week_low": round(year_low, 2),
            "50_day_avg": round(info.get("fiftyDayAverage", current_price), 2),
            "200_day_avg": round(info.get("twoHundredDayAverage", current_price), 2),
            "avg_volume": int(avg_volume),
            "price_change_1d": round(price_change_1d, 2),
            "price_change_1m": round(price_change_1m, 2),
            "dividend_yield": round(info.get("dividendYield", 0) * 100, 2) if info.get("dividendYield") else 0,
            "beta": round(info.get("beta", 1), 2) if info.get("beta") else None,
            "analyst_recommendation": info.get("recommendationKey", "hold"),
            "target_mean_price": round(info.get("targetMeanPrice", 0), 2) if info.get("targetMeanPrice") else None,
        }
        
        logger.info(f"✅ Data Agent: Successfully collected {len(live_data)} metrics")
        return live_data
        
    except Exception as e:
        logger.error(f"❌ Data Agent error: {str(e)}")
        raise


# Agent: AI Analysis
def generate_ai_analysis(ticker: str, live_data: Dict[str, Any]) -> str:
    """
    Autonomous AI Reasoning Agent
    
    Capabilities:
    - Multi-step reasoning
    - Financial analysis
    - Risk assessment
    - Investment recommendation
    """
    if not anthropic_client:
        return generate_fallback_analysis(ticker, live_data)
    
    try:
        logger.info(f"🧠 Reasoning Agent: Analyzing {ticker}")
        
        # Create analysis prompt
        prompt = f"""You are a professional stock analyst. Analyze this stock and provide a comprehensive investment report.

Stock: {ticker} - {live_data.get('company_name')}
Sector: {live_data.get('sector')} | Industry: {live_data.get('industry')}

FINANCIAL METRICS:
- Current Price: ${live_data.get('current_price')} {live_data.get('currency')}
- Market Cap: ${live_data.get('market_cap', 0):,.0f}
- P/E Ratio: {live_data.get('pe_ratio', 'N/A')}
- Forward P/E: {live_data.get('forward_pe', 'N/A')}
- PEG Ratio: {live_data.get('peg_ratio', 'N/A')}
- Price to Book: {live_data.get('price_to_book', 'N/A')}

PROFITABILITY:
- Profit Margin: {live_data.get('profit_margin', 'N/A')}%
- Operating Margin: {live_data.get('operating_margin', 'N/A')}%
- ROE: {live_data.get('roe', 'N/A')}%

GROWTH:
- Revenue Growth: {live_data.get('revenue_growth', 'N/A')}%
- Earnings Growth: {live_data.get('earnings_growth', 'N/A')}%

DEBT & RISK:
- Debt to Equity: {live_data.get('debt_to_equity', 'N/A')}
- Beta: {live_data.get('beta', 'N/A')}

PRICE MOMENTUM:
- 1-Day Change: {live_data.get('price_change_1d', 0)}%
- 1-Month Change: {live_data.get('price_change_1m', 0)}%
- 52-Week Range: ${live_data.get('52_week_low')} - ${live_data.get('52_week_high')}
- 50-Day Avg: ${live_data.get('50_day_avg')}
- 200-Day Avg: ${live_data.get('200_day_avg')}

ANALYST DATA:
- Recommendation: {live_data.get('analyst_recommendation', 'N/A')}
- Target Price: ${live_data.get('target_mean_price', 'N/A')}

Provide a detailed analysis with:
1. Valuation Assessment (Is it overvalued/undervalued?)
2. Financial Health (Profitability, margins, debt)
3. Growth Prospects (Revenue and earnings trends)
4. Risk Factors (What could go wrong?)
5. Price Momentum (Technical analysis)
6. Final Recommendation (BUY/HOLD/SELL with confidence level)
7. Entry/Exit Strategy (Specific price targets)

Be specific, data-driven, and actionable."""

        # Call Claude AI
        message = anthropic_client.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=2000,
            messages=[{
                "role": "user",
                "content": prompt
            }]
        )
        
        analysis = message.content[0].text
        logger.info(f"✅ Reasoning Agent: Generated {len(analysis)} character analysis")
        
        return analysis
        
    except Exception as e:
        logger.error(f"❌ Reasoning Agent error: {str(e)}")
        return generate_fallback_analysis(ticker, live_data)


def generate_fallback_analysis(ticker: str, live_data: Dict[str, Any]) -> str:
    """Fallback analysis when AI is unavailable"""
    pe = live_data.get('pe_ratio', 0)
    margin = live_data.get('profit_margin', 0)
    growth = live_data.get('revenue_growth', 0)
    
    recommendation = "HOLD"
    if pe and pe < 20 and margin and margin > 15 and growth and growth > 10:
        recommendation = "BUY"
    elif pe and pe > 40 or margin and margin < 5:
        recommendation = "SELL"
    
    return f"""AUTOMATED ANALYSIS FOR {ticker}

VALUATION: P/E Ratio of {pe} suggests {"attractive valuation" if pe and pe < 25 else "premium valuation"}

PROFITABILITY: Profit margin of {margin}% indicates {"strong" if margin and margin > 15 else "moderate"} profitability

GROWTH: Revenue growth of {growth}% shows {"impressive" if growth and growth > 10 else "steady"} expansion

RECOMMENDATION: {recommendation}

Note: This is an automated analysis. For detailed insights, ensure ANTHROPIC_API_KEY is configured."""


# API Endpoints
@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "Sentinel AI Stock Analysis",
        "version": "1.0.0",
        "status": "operational",
        "agents": {
            "data_collection": "active",
            "ai_reasoning": "active" if anthropic_client else "fallback_mode",
            "orchestration": "active"
        }
    }


@app.get("/health")
async def health():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "ai_available": bool(anthropic_client)
    }


@app.post("/api/generate-report", response_model=AnalysisResponse)
async def generate_report(request: AnalysisRequest):
    """
    Generate complete stock analysis report
    
    This is the main orchestration endpoint that coordinates
    the autonomous agents to produce actionable insights.
    """
    try:
        ticker = request.company_name.strip().upper()
        logger.info(f"📊 Analysis Request: {ticker} from {request.email}")
        
        # Step 1: Data Collection Agent
        live_data = fetch_stock_data(ticker)
        
        # Step 2: AI Reasoning Agent
        analysis = generate_ai_analysis(ticker, live_data)
        
        # Step 3: Return synthesized results
        logger.info(f"✅ Analysis Complete: {ticker}")
        
        return AnalysisResponse(
            success=True,
            live_data=live_data,
            report=analysis
        )
        
    except Exception as e:
        logger.error(f"❌ Analysis Failed: {str(e)}")
        return AnalysisResponse(
            success=False,
            error=str(e)
        )


@app.get("/api/verify-price/{ticker}")
async def verify_price(ticker: str):
    """Quick price check endpoint"""
    try:
        stock = yf.Ticker(ticker)
        info = stock.info
        hist = stock.history(period="1d")
        
        return {
            "ticker": ticker.upper(),
            "price": round(hist['Close'].iloc[-1], 2) if not hist.empty else None,
            "company": info.get("longName", ticker),
            "valid": not hist.empty
        }
    except Exception as e:
        return {"error": str(e), "valid": False}


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
