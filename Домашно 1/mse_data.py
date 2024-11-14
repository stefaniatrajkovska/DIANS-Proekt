from bs4 import BeautifulSoup
import pandas as pd
import asyncio
import aiohttp
from datetime import datetime
import time
from aiohttp import ClientSession, TCPConnector

# Поставуваме почеток на тајмерот
start_time = time.time()

# Основен URL за веб-страницата
base_url = 'https://www.mse.mk/mk/stats/symbolhistory/'

# Екстракција на сите валидни шифри (без броеви)
def get_valid_options():
    from selenium import webdriver
    chrome_options = webdriver.ChromeOptions()
    chrome_options.add_argument("--headless")
    browser = webdriver.Chrome(options=chrome_options)
    browser.get(base_url + 'alk')
    time.sleep(2)

    page_html = browser.page_source
    soup = BeautifulSoup(page_html, 'html.parser')
    options = soup.find_all('option', {'value': True})
    valid_options = [option.get('value') for option in options if option.get('value') and not any(char.isdigit() for char in option.get('value'))]
    browser.quit()
    return valid_options

# Генерирање временски интервали од по една година за последните 10 години
def get_date_intervals():
    end_date = datetime.now()
    intervals = []
    for i in range(10):
        start_date = end_date.replace(year=end_date.year - 1)
        intervals.append((start_date.strftime('%d.%m.%Y'), end_date.strftime('%d.%m.%Y')))
        end_date = start_date
    return intervals

# Асинхрони функции за вчитување на податоци со рачно повторување
async def fetch_data(session, code, start_date, end_date, max_retries=3):
    url = f"{base_url}{code.lower()}?FromDate={start_date}&ToDate={end_date}"
    for attempt in range(max_retries):
        try:
            async with session.get(url) as response:
                page_html = await response.text()
                soup = BeautifulSoup(page_html, 'html.parser')
                rows = soup.select("#resultsTable tbody > tr")

                row_data = []
                for row in rows:
                    cells = row.find_all("td")
                    if len(cells) < 9:
                        continue
                    data = {
                        "Шифра": code,
                        "Дата": cells[0].text.strip(),
                        "ЦенаПоследна": cells[1].text.strip(),
                        "Максимум": cells[2].text.strip(),
                        "Минимум": cells[3].text.strip(),
                        "ПросечнаЦена": cells[4].text.strip(),
                        "Промена%": cells[5].text.strip(),
                        "Количина": cells[6].text.strip(),
                        "Промет": cells[7].text.strip(),
                        "Вкупно": cells[8].text.strip()
                    }
                    if all(data.values()):
                        row_data.append(data)

                # Додавање на новите податоци директно во CSV за да се избегне задржување на меморија
                pd.DataFrame(row_data).to_csv("mse_data.csv", mode='a', index=False, header=False)
                return row_data
        except Exception as e:

            await asyncio.sleep(2)

    return []

async def main():
    valid_options = get_valid_options()
    date_intervals = get_date_intervals()

    # Поставување на TCPConnector со ограничување на паралелни барања
    connector = TCPConnector(limit=20)
    async with ClientSession(connector=connector) as session:
        tasks = []
        for code in valid_options:
            for start_date, end_date in date_intervals:
                tasks.append(fetch_data(session, code, start_date, end_date))

        # Континуирано собирање и обработка на резултатите од задачите за да се избегне преоптоварување
        for task in asyncio.as_completed(tasks):
            await task

# Креирање на CSV со наслов пред да се пополнат податоците
pd.DataFrame(columns=["Шифра", "Дата", "ЦенаПоследна", "Максимум", "Минимум", "ПросечнаЦена", "Промена%", "Количина", "Промет", "Вкупно"]).to_csv("mse_data.csv", index=False)

# Извршување на асинхроната функција
asyncio.run(main())

# Пресметка и печатење на времето на извршување
end_time = time.time()
execution_time = end_time - start_time
print(f"Вкупно време на извршување: {execution_time:.2f} секунди")

