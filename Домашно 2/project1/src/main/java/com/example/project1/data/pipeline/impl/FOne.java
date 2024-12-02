package com.example.project1.data.pipeline.impl;

import com.example.project1.data.pipeline.Filter;
import com.example.project1.model.CompanyIssuer;
import com.example.project1.repository.CompanyIssuerRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;

public class FOne implements Filter<List<CompanyIssuer>> {

    private final CompanyIssuerRepository companyIssuerRepository;

    public FOne(CompanyIssuerRepository companyIssuerRepository) {
        this.companyIssuerRepository = companyIssuerRepository;
    }

    private static final String STOCK_MARKET_URL = "https://www.mse.mk/mk/stats/symbolhistory/kmb";

    @Override
    public List<CompanyIssuer> execute(List<CompanyIssuer> input) throws IOException {
        Document document = Jsoup.connect(STOCK_MARKET_URL).get();
        Element selectMenu = document.select("select#Code").first();

        if (selectMenu != null) {
            Elements options = selectMenu.select("option");
            for (Element option : options) {
                String code = option.attr("value");
                if (!code.isEmpty() && code.matches("^[a-zA-Z]+$")) {
                    if (companyIssuerRepository.findByCompanyCode(code).isEmpty()) {
                        companyIssuerRepository.save(new CompanyIssuer(code));
                    }
                }
            }
        }

        return companyIssuerRepository.findAll();
    }
}
