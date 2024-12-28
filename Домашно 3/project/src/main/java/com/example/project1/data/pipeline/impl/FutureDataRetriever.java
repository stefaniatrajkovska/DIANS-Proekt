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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FutureDataRetriever implements DataFilter<List<CorporationIssuer>> {

    private final CorporationIssuerRepository companyIssuerRepository;
    private final CorporationDataRepository companyDataRepository;

    private static final String URL = "https://www.mse.mk/mk/stats/symbolhistory/";

    public FutureDataRetriever(CorporationIssuerRepository companyIssuerRepository, CorporationDataRepository companyDataRepository) {
        this.companyIssuerRepository = companyIssuerRepository;
        this.companyDataRepository = companyDataRepository;
    }

    @Override
    public List<CorporationIssuer> execute(List<CorporationIssuer> input) throws IOException {
        List<CorporationIssuer> companies = new ArrayList<>();

        input.forEach(company -> {
            try {
                if (company.getDateLastUpdated() == null && company.getCorporationCode().equals("ALKB")) {
                    processCompanyHistoricalData(company);
                } else {
                    companies.add(company);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error processing company: " + company.getCorporationCode(), e);
            }
        });

        return companies;
    }

    private void processCompanyHistoricalData(CorporationIssuer company) throws IOException {
        for (int i = 1; i <= 10; i++) {
            LocalDate fromDate = LocalDate.now().minusYears(i);
            LocalDate toDate = LocalDate.now().minusYears(i - 1);
            addHistoricalData(company, fromDate, toDate);
        }
    }

    private void addHistoricalData(CorporationIssuer company, LocalDate fromDate, LocalDate toDate) throws IOException {
        Document document = fetchDocument(company.getCorporationCode(), fromDate, toDate);
        Element table = document.select("table#resultsTable").first();

        if (table != null) {
            processTableRows(table, company);
        }

        companyIssuerRepository.save(company);
    }

    private Document fetchDocument(String companyCode, LocalDate fromDate, LocalDate toDate) throws IOException {
        Connection.Response response = Jsoup.connect(URL + companyCode)
                .data("FromDate", fromDate.toString())
                .data("ToDate", toDate.toString())
                .method(Connection.Method.POST)
                .execute();
        return response.parse();
    }

    private void processTableRows(Element table, CorporationIssuer company) {
        Elements rows = table.select("tbody tr");

        for (Element row : rows) {
            Elements columns = row.select("td");

            if (!columns.isEmpty()) {
                processRow(columns, company);
            }
        }
    }

    private void processRow(Elements columns, CorporationIssuer company) {
        LocalDate date = DataFormatter.parseDate(columns.get(0).text(), "d.M.yyyy");

        if (companyDataRepository.findByDateAndCorporationIssuer(date, company).isEmpty()) {
            CorporationData companyData = extractCorporationData(columns, company, date);

            if (companyData != null) {
                updateLastUpdatedDate(company, date);
                companyDataRepository.save(companyData);
                company.getHistoricalRecords().add(companyData);
            }
        }
    }

    private CorporationData extractCorporationData(Elements columns, CorporationIssuer company, LocalDate date) {
        NumberFormat format = NumberFormat.getInstance(Locale.GERMANY);

        Double lastTransactionPrice = DataFormatter.parseDouble(columns.get(1).text(), format);
        Double maxPrice = DataFormatter.parseDouble(columns.get(2).text(), format);
        Double minPrice = DataFormatter.parseDouble(columns.get(3).text(), format);
        Double averagePrice = DataFormatter.parseDouble(columns.get(4).text(), format);
        Double percentageChange = DataFormatter.parseDouble(columns.get(5).text(), format);
        Integer quantity = DataFormatter.parseInteger(columns.get(6).text(), format);
        Integer turnoverBest = DataFormatter.parseInteger(columns.get(7).text(), format);
        Integer totalTurnover = DataFormatter.parseInteger(columns.get(8).text(), format);

        if (maxPrice != null) {
            return new CorporationData(
                    date, lastTransactionPrice, maxPrice, minPrice, averagePrice,
                    percentageChange, quantity, turnoverBest, totalTurnover
            );
        }

        return null;
    }

    private void updateLastUpdatedDate(CorporationIssuer company, LocalDate date) {
        if (company.getDateLastUpdated() == null || company.getDateLastUpdated().isBefore(date)) {
            company.setDateLastUpdated(date);
        }
    }




}
