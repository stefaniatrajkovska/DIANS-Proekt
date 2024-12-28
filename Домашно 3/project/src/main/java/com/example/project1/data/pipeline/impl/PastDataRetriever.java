package com.example.project1.data.pipeline.impl;

import com.example.project1.data.DataFormatter;
import com.example.project1.data.pipeline.DataFilter;
import com.example.project1.model.CorporationIssuer;
import com.example.project1.model.CorporationData;
import com.example.project1.repository.CorporationDataRepository;
import com.example.project1.repository.CorporationIssuerRepository;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class PastDataRetriever implements DataFilter<List<CorporationIssuer>> {

    private final CorporationIssuerRepository companyIssuerRepository;
    private final CorporationDataRepository companyDataRepository;

    private static final String URL = "https://www.mse.mk/mk/stats/symbolhistory/";

    public PastDataRetriever(CorporationIssuerRepository companyIssuerRepository, CorporationDataRepository companyDataRepository) {
        this.companyIssuerRepository = companyIssuerRepository;
        this.companyDataRepository = companyDataRepository;
    }

    public List<CorporationIssuer> execute(List<CorporationIssuer> input) throws IOException {
        for (CorporationIssuer company : input) {
            fetchAndProcessHistoricalData(company);
        }

        return null;
    }

    private void fetchAndProcessHistoricalData(CorporationIssuer company) throws IOException {
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = LocalDate.now().plusYears(1);
        addHistoricalData(company, fromDate, toDate);
    }

    private void addHistoricalData(CorporationIssuer company, LocalDate fromDate, LocalDate toDate) throws IOException {
        Connection.Response response = Jsoup.connect(URL + company.getCorporationCode())
                .data("FromDate", fromDate.toString())
                .data("ToDate", toDate.toString())
                .method(Connection.Method.POST)
                .execute();

        Document document = response.parse();
        Element table = document.select("table#resultsTable").first();

        if (table == null) return;

        Elements rows = table.select("tbody tr");
        NumberFormat format = NumberFormat.getInstance(Locale.GERMANY);

        for (Element row : rows) {
            Elements columns = row.select("td");
            if (!columns.isEmpty()) {
                processRow(columns, company, format);
            }
        }

        companyIssuerRepository.save(company);
    }

    private void processRow(Elements columns, CorporationIssuer company, NumberFormat format) {
        LocalDate date = DataFormatter.parseDate(columns.get(0).text(), "d.M.yyyy");

        if (companyDataRepository.findByDateAndCorporationIssuer(date, company).isEmpty()) {
            Double lastTransactionPrice = DataFormatter.parseDouble(columns.get(1).text(), format);
            Double maxPrice = DataFormatter.parseDouble(columns.get(2).text(), format);
            Double minPrice = DataFormatter.parseDouble(columns.get(3).text(), format);
            Double averagePrice = DataFormatter.parseDouble(columns.get(4).text(), format);
            Double percentageChange = DataFormatter.parseDouble(columns.get(5).text(), format);
            Integer quantity = DataFormatter.parseInteger(columns.get(6).text(), format);
            Integer turnoverBest = DataFormatter.parseInteger(columns.get(7).text(), format);
            Integer totalTurnover = DataFormatter.parseInteger(columns.get(8).text(), format);

            if (maxPrice != null) {
                updateCompanyLastUpdated(company, date);
                saveCorporationData(company, date, lastTransactionPrice, maxPrice, minPrice, averagePrice,
                        percentageChange, quantity, turnoverBest, totalTurnover);
            }
        }
    }

    private void updateCompanyLastUpdated(CorporationIssuer company, LocalDate date) {
        if (company.getDateLastUpdated() == null || company.getDateLastUpdated().isBefore(date)) {
            company.setDateLastUpdated(date);
        }
    }

    private void saveCorporationData(CorporationIssuer company, LocalDate date, Double lastTransactionPrice,
                                     Double maxPrice, Double minPrice, Double averagePrice, Double percentageChange,
                                     Integer quantity, Integer turnoverBest, Integer totalTurnover) {
        CorporationData companyDataModel = new CorporationData(
                date, lastTransactionPrice, maxPrice, minPrice, averagePrice, percentageChange,
                quantity, turnoverBest, totalTurnover);

        companyDataModel.setCorporationIssuer(company);
        companyDataRepository.save(companyDataModel);
        company.getHistoricalRecords().add(companyDataModel);
    }



}
