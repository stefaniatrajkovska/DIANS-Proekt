package com.example.project1.data.pipeline.impl;

import com.example.project1.data.ParserForData;
import com.example.project1.data.pipeline.Filter;
import com.example.project1.model.CompanyIssuer;
import com.example.project1.model.DayPrice;
import com.example.project1.repository.CompanyIssuerRepository;
import com.example.project1.repository.DayPriceRepository;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class FThree implements Filter<List<CompanyIssuer>> {

    private final CompanyIssuerRepository companyIssuerRepository;
    private final DayPriceRepository dayPriceRepository;

    private static final String HISTORICAL_DATA_URL = "https://www.mse.mk/mk/stats/symbolhistory/";

    public FThree(CompanyIssuerRepository companyIssuerRepository, DayPriceRepository dayPriceRepository) {
        this.companyIssuerRepository = companyIssuerRepository;
        this.dayPriceRepository = dayPriceRepository;
    }

    public List<CompanyIssuer> execute(List<CompanyIssuer> input) throws IOException, ParseException {

        for (CompanyIssuer companyIssuer : input) {
            LocalDate fromDate = LocalDate.now();
            LocalDate toDate = LocalDate.now().plusYears(1);
            addHistoricalData(companyIssuer, fromDate, toDate);
        }

        return null;
    }

    private void addHistoricalData(CompanyIssuer companyIssuer, LocalDate fromDate, LocalDate toDate) throws IOException {
        Connection.Response response = Jsoup.connect(HISTORICAL_DATA_URL + companyIssuer.getCompanyCode())
                .data("FromDate", fromDate.toString())
                .data("ToDate", toDate.toString())
                .method(Connection.Method.POST)
                .execute();

        Document document = response.parse();

        Element table = document.select("table#resultsTable").first();

        if (table != null) {
            Elements rows = table.select("tbody tr");

            for (Element row : rows) {
                Elements columns = row.select("td");

                if (columns.size() > 0) {
                    LocalDate date = ParserForData.parseDate(columns.get(0).text(), "d.M.yyyy");

                    if (dayPriceRepository.findByDateAndCompany(date, companyIssuer).isEmpty()) {

                        NumberFormat format = NumberFormat.getInstance(Locale.GERMANY);

                        Double lastTransactionPrice = ParserForData.parseDouble(columns.get(1).text(), format);
                        Double maxPrice = ParserForData.parseDouble(columns.get(2).text(), format);
                        Double minPrice = ParserForData.parseDouble(columns.get(3).text(), format);
                        Double averagePrice = ParserForData.parseDouble(columns.get(4).text(), format);
                        Double percentageChange = ParserForData.parseDouble(columns.get(5).text(), format);
                        Integer quantity = ParserForData.parseInteger(columns.get(6).text(), format);
                        Integer turnoverBest = ParserForData.parseInteger(columns.get(7).text(), format);
                        Integer totalTurnover = ParserForData.parseInteger(columns.get(8).text(), format);

                        if (maxPrice != null) {

                            if (companyIssuer.getLastUpdated() == null || companyIssuer.getLastUpdated().isBefore(date)) {
                                companyIssuer.setLastUpdated(date);
                            }

                            DayPrice dayPrice = new DayPrice(
                                    date, lastTransactionPrice, maxPrice, minPrice, averagePrice, percentageChange,
                                    quantity, turnoverBest, totalTurnover);
                            dayPrice.setCompanyIssuer(companyIssuer);
                            dayPriceRepository.save(dayPrice);
                            companyIssuer.getHistoricalData().add(dayPrice);
                        }
                    }
                }
            }
        }
        companyIssuerRepository.save(companyIssuer);
    }


}
