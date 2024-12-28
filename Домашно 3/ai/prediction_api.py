from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import pandas as pd
from statsmodels.tsa.arima.model import ARIMA
from datetime import datetime

app = FastAPI()


# Модел за еден запис на историски податоци
class HistoricalDataItem(BaseModel):
    date: str  # Очекуван формат на датата: 'YYYY-MM-DD'
    average_price: float  # Средна цена во тој ден


# Модел за листа на историски податоци
class HistoricalData(BaseModel):
    data: List[HistoricalDataItem]


def preprocess_data(historical_data: pd.DataFrame) -> pd.DataFrame:
    """
    Превработува историски податоци: конвертира датум во datetime објект,
    поставува го како индекс и чисти ги празните вредности.
    """
    # Конвертирање на датата во datetime формат
    historical_data['date'] = pd.to_datetime(historical_data['date'])
    historical_data.set_index('date', inplace=True)

    # Чистење на празните вредности
    historical_data = historical_data.dropna()

    return historical_data


def predict_next_month_price(historical_data: pd.DataFrame) -> float:
    """
    Предвидува просечната цена за следниот месец користејќи ARIMA модел.
    Ако има помалку од 30 податоци, нема да може да направи точна предвидување.
    """
    # Проверка дали има доволно податоци
    if len(historical_data) < 30:
        raise ValueError("Not enough data to make a reliable prediction.")

    # Применување на ARIMA модел
    model = ARIMA(historical_data['average_price'], order=(5, 1, 0))  # Параметрите на ARIMA можат да се подесат
    model_fit = model.fit()

    # Прогнозирање на наредните 30 дена (следниот месец)
    forecast = model_fit.forecast(steps=30)

    # Враќање на просечната предвидена цена за следниот месец
    return forecast.mean()


@app.post("/predict-next-month-price/")
async def predict_next_month_price_endpoint(historical_data: HistoricalData):
    """
    Ендпоинт за предвидување на цената за следниот месец.
    Прифаќа листа на историски податоци и враќа прогнозирана цена.
    """
    try:
        # Претворање на податоците во DataFrame
        data = pd.DataFrame([item.dict() for item in historical_data.data])

        # Препроцесирање на податоците и предвидување на цената
        processed_data = preprocess_data(data)
        predicted_price = predict_next_month_price(processed_data)

        # Враќање на прогнозата
        return {"predicted_next_month_price": predicted_price}
    except Exception as e:
        # Враќање на грешка ако нешто не оди како што треба
        raise HTTPException(status_code=500, detail=str(e))

# Започнете ја апликацијата со Uvicorn
# Команда за стартирање: uvicorn prediction_api:app --reload
