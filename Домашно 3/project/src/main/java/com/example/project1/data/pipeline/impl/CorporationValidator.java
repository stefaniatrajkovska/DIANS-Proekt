package com.example.project1.data.pipeline.impl;

import com.example.project1.data.pipeline.DataFilter;
import com.example.project1.model.CorporationIssuer;
import com.example.project1.repository.CorporationIssuerRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;

public class CorporationValidator implements DataFilter<List<CorporationIssuer>> {

    private final CorporationIssuerRepository issuerRepository;

    public CorporationValidator(CorporationIssuerRepository issuerRepository) {
        this.issuerRepository = issuerRepository;
    }

    private static final String MARKET_URL = "https://www.mse.mk/mk/stats/symbolhistory/kmb";

    @Override
    public List<CorporationIssuer> execute(List<CorporationIssuer> existingIssuers) throws IOException {
        Document htmlDocument = Jsoup.connect(MARKET_URL).get();
        Element dropdown = htmlDocument.select("select#Code").first();

        if (dropdown != null) {
            Elements options = dropdown.select("option");
            for (Element optionElement : options) {
                String issuerCode = optionElement.attr("value");
                if (!issuerCode.isEmpty() && issuerCode.matches("^[a-zA-Z]+$")) {
                    if (issuerRepository.findByCorporationCode(issuerCode).isEmpty()) {
                        issuerRepository.save(new CorporationIssuer(issuerCode));
                    }
                }
            }
        }

        return issuerRepository.findAll();
    }
}
