from flask import Flask, request, jsonify
import numpy as np
import requests
from bs4 import BeautifulSoup
from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer
from typing import List
import pandas as pd

app = Flask(__name__)


def compute_rsi(data, period=14):
    price_changes = data['close'].diff()
    gains = (price_changes.where(price_changes > 0, 0)).rolling(window=period).mean()
    losses = (-price_changes.where(price_changes < 0, 0)).rolling(window=period).mean()
    relative_strength = gains / losses
    return 100 - (100 / (1 + relative_strength))


def compute_macd(data, fast_period=12, slow_period=26, signal_period=9):
    fast_ema = data['close'].ewm(span=fast_period).mean()
    slow_ema = data['close'].ewm(span=slow_period).mean()
    macd_line = fast_ema - slow_ema
    signal_line = macd_line.ewm(span=signal_period).mean()
    return macd_line, signal_line, macd_line - signal_line


def compute_stochastic(data, period=14):
    lowest_low = data['low'].rolling(window=period).min()
    highest_high = data['high'].rolling(window=period).max()
    return 100 * ((data['close'] - lowest_low) / (highest_high - lowest_low))


def compute_cci(data, period=20):
    typical_price = (data['high'] + data['low'] + data['close']) / 3
    moving_average = typical_price.rolling(window=period).mean()
    mean_absolute_deviation = (typical_price - moving_average).abs().rolling(window=period).mean()
    return (typical_price - moving_average) / (0.015 * mean_absolute_deviation)


def compute_atr(data, period=14):
    data['H-L'] = data['high'] - data['low']
    data['H-PC'] = abs(data['high'] - data['close'].shift(1))
    data['L-PC'] = abs(data['low'] - data['close'].shift(1))
    data['TR'] = data[['H-L', 'H-PC', 'L-PC']].max(axis=1)
    return data['TR'].rolling(window=period).mean()


def compute_sma(data, window=10):
    return data['close'].rolling(window=window).mean()


def compute_ema(data, span=10):
    return data['close'].ewm(span=span, adjust=False).mean()


def compute_wma(data, window=10):
    weights = np.arange(1, window + 1)
    return data['close'].rolling(window=window).apply(lambda prices: np.dot(prices, weights) / weights.sum(), raw=True)


def compute_hma(data, period=14):
    half_period = period // 2
    sqrt_period = int(np.sqrt(period))
    wma_half = compute_wma(data, window=half_period)
    wma_full = compute_wma(data, window=period)
    hull_ma = compute_wma(pd.DataFrame({'close': 2 * wma_half - wma_full}), window=sqrt_period)
    return hull_ma[0]


def compute_vwap(data):
    return (data['close'] * data['volume']).cumsum() / data['volume'].cumsum()


def generate_trade_signal(data, period=14):
    last_record = data.iloc[-1]
    indicators = {
        "RSI": 'Buy' if last_record[f'RSI_{period}'] < 30 else (
            'Sell' if last_record[f'RSI_{period}'] > 70 else 'Hold'),
        "MACD": 'Buy' if last_record[f'MACD_hist_{period}'] > 0 else (
            'Sell' if last_record[f'MACD_hist_{period}'] < 0 else 'Hold'),
        "Stochastic": 'Buy' if last_record[f'Stochastic_{period}'] < 20 else (
            'Sell' if last_record[f'Stochastic_{period}'] > 80 else 'Hold'),
        "CCI": 'Sell' if last_record[f'CCI_{period}'] > 100 else (
            'Buy' if last_record[f'CCI_{period}'] < -100 else 'Hold'),
        "ATR": 'Sell' if last_record[f'ATR_{period}'] > 1.5 else 'Buy'
    }

    moving_avg_signal = 'Buy' if (last_record[f'SMA_{period}'] > last_record['SMA_50'] and
                                  last_record[f'EMA_{period}'] > last_record['SMA_50'] and
                                  last_record[f'WMA_{period}'] > last_record['SMA_50'] and
                                  last_record[f'HMA_{period}'] > last_record['SMA_50'] and
                                  last_record[f'VWAP_{period}'] > last_record['SMA_50']) else 'Sell'

    final_signal = 'Buy' if all(val == 'Buy' for val in indicators.values()) and moving_avg_signal == 'Buy' else \
        ('Sell' if all(val == 'Sell' for val in indicators.values()) and moving_avg_signal == 'Sell' else 'Hold')
    return final_signal


# Функции за анализа на новинарски текстови

def fetch_company_news(company_code):
    url = f"https://www.mse.mk/mk/search/{company_code}"
    response = requests.get(url)
    if response.status_code == 200:
        soup = BeautifulSoup(response.text)
        # Може да се додадат други детали од новинскиот извор ако се потребни
        return soup.text
    return "No news found for this company."


def analyze_sentiment(news_text):
    analyzer = SentimentIntensityAnalyzer()
    sentiment_score = analyzer.polarity_scores(news_text)
    return sentiment_score


# API за добивање на аналитички податоци
@app.route('/analyze_stock', methods=['POST'])
def analyze_stock():
    company_code = request.json.get('company_code')
    stock_data = request.json.get('stock_data')  # Очекуваме податоци за акциите како DataFrame
    data_df = pd.DataFrame(stock_data)

    rsi_values = compute_rsi(data_df)
    macd_values, signal_values, macd_histogram = compute_macd(data_df)
    stochastic_values = compute_stochastic(data_df)
    cci_values = compute_cci(data_df)
    atr_values = compute_atr(data_df)
    sma_values = compute_sma(data_df)
    ema_values = compute_ema(data_df)
    wma_values = compute_wma(data_df)
    hma_values = compute_hma(data_df)
    vwap_values = compute_vwap(data_df)

    signal = generate_trade_signal(data_df)

    news_text = fetch_company_news(company_code)
    sentiment = analyze_sentiment(news_text)

    result = {
        "RSI": rsi_values.tolist(),
        "MACD": macd_values.tolist(),
        "MACD_signal": signal_values.tolist(),
        "Stochastic": stochastic_values.tolist(),
        "CCI": cci_values.tolist(),
        "ATR": atr_values.tolist(),
        "SMA": sma_values.tolist(),
        "EMA": ema_values.tolist(),
        "WMA": wma_values.tolist(),
        "HMA": hma_values.tolist(),
        "VWAP": vwap_values.tolist(),
        "Trade Signal": signal,
        "Sentiment": sentiment
    }

    return jsonify(result)


if __name__ == '__main__':
    app.run(debug=True)
