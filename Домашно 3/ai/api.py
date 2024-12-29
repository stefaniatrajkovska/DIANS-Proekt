from flask import Flask, request, jsonify
import numpy as np
import requests
from bs4 import BeautifulSoup
from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer
from typing import List
import pandas as pd

# Иницијализација на Flask апликацијата
app = Flask(__name__)

# Функција за пресметување на RSI (Relative Strength Index)
def compute_rsi(data, period=14):
    price_changes = data['close'].diff()  # Промени во цената
    gains = (price_changes.where(price_changes > 0, 0)).rolling(window=period).mean()  # Просечен добивок
    losses = (-price_changes.where(price_changes < 0, 0)).rolling(window=period).mean()  # Просечен загуба
    relative_strength = gains / losses  # Однос на добивка/загуба
    return 100 - (100 / (1 + relative_strength))  # Формула за RSI

# Функција за пресметување на MACD (Moving Average Convergence Divergence)
def compute_macd(data, fast_period=12, slow_period=26, signal_period=9):
    fast_ema = data['close'].ewm(span=fast_period).mean()  # Брза ЕМА
    slow_ema = data['close'].ewm(span=slow_period).mean()  # Спора ЕМА
    macd_line = fast_ema - slow_ema  # MACD линија
    signal_line = macd_line.ewm(span=signal_period).mean()  # Signal линија
    return macd_line, signal_line, macd_line - signal_line  # MACD хистограм

# Функција за пресметување на Стохастик
def compute_stochastic(data, period=14):
    lowest_low = data['low'].rolling(window=period).min()  # Најниска цена
    highest_high = data['high'].rolling(window=period).max()  # Највисока цена
    return 100 * ((data['close'] - lowest_low) / (highest_high - lowest_low))  # Формула за стохастик

# Функција за пресметување на CCI (Commodity Channel Index)
def compute_cci(data, period=20):
    typical_price = (data['high'] + data['low'] + data['close']) / 3  # Типична цена
    moving_average = typical_price.rolling(window=period).mean()  # Подвижен просек
    mean_absolute_deviation = (typical_price - moving_average).abs().rolling(window=period).mean()  # Средно отстапување
    return (typical_price - moving_average) / (0.015 * mean_absolute_deviation)  # Формула за CCI

# Функција за пресметување на ATR (Average True Range)
def compute_atr(data, period=14):
    data['H-L'] = data['high'] - data['low']  # Опсег висока-ниска цена
    data['H-PC'] = abs(data['high'] - data['close'].shift(1))  # Опсег висока-претходна цена
    data['L-PC'] = abs(data['low'] - data['close'].shift(1))  # Опсег ниска-претходна цена
    data['TR'] = data[['H-L', 'H-PC', 'L-PC']].max(axis=1)  # True Range (TR)
    return data['TR'].rolling(window=period).mean()  # Просечен TR (ATR)

# Слични објаснувања важат и за останатите индикатори (SMA, EMA, WMA, HMA, VWAP).

# Функција за генерирање сигнал за тргување врз основа на индикаторите
def generate_trade_signal(data, period=14):
    last_record = data.iloc[-1]  # Последен запис
    # Проверка на RSI, MACD, Стохастик, CCI и ATR за сигнал
    indicators = {
        "RSI": 'Buy' if last_record[f'RSI_{period}'] < 30 else (
            'Sell' if last_record[f'RSI_{period}'] > 70 else 'Hold'),
        # ... останатите индикатори
    }
    # Краен сигнал врз основа на сите индикатори
    final_signal = 'Buy' if all(val == 'Buy' for val in indicators.values()) else 'Hold'
    return final_signal

# Функции за анализа на новинарски текстови (сентимент анализа)
def fetch_company_news(company_code):
    url = f"https://www.mse.mk/mk/search/{company_code}"  # УРЛ за пребарување новини
    response = requests.get(url)
    if response.status_code == 200:
        soup = BeautifulSoup(response.text)  # Парсирање на HTML
        return soup.text
    return "No news found for this company."

def analyze_sentiment(news_text):
    analyzer = SentimentIntensityAnalyzer()
    return analyzer.polarity_scores(news_text)  # Резултати за сентимент

# API ендпоинт за анализа на акции
@app.route('/analyze_stock', methods=['POST'])
def analyze_stock():
    company_code = request.json.get('company_code')  # Код на компанијата
    stock_data = request.json.get('stock_data')  # Податоци за акции
    data_df = pd.DataFrame(stock_data)

    # Пресметување на сите индикатори
    rsi_values = compute_rsi(data_df)
    # ... останати индикатори

    # Генерирање сигнал
    signal = generate_trade_signal(data_df)

    # Анализа на новини
    news_text = fetch_company_news(company_code)
    sentiment = analyze_sentiment(news_text)

    # Резултат
    result = {
        "RSI": rsi_values.tolist(),
        # ... останати индикатори
        "Trade Signal": signal,
        "Sentiment": sentiment
    }

    return jsonify(result)

if __name__ == '__main__':
    app.run(debug=True)  # Стартување на Flask апликацијата
