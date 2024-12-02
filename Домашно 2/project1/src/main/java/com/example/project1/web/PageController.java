package com.example.project1.web;

import com.example.project1.model.CompanyIssuer;
import com.example.project1.model.DayPrice;
import com.example.project1.service.CompanyIssuerService;
import com.example.project1.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final CompanyIssuerService companyIssuerService;
    private final AIService AIService;

    @GetMapping("/")
    public String getIndexPage(Model model) {
        model.addAttribute("companies", companyIssuerService.findAll());
        return "index";
    }

    @GetMapping("/company")
    public String getCompanyPage(@RequestParam(name = "companyId") Long companyId, Model model) throws Exception {
        List<Map<String, Object>> companyData = new ArrayList<>();
        CompanyIssuer companyIssuer = companyIssuerService.findById(companyId);

        Map<String, Object> data = new HashMap<>();
        data.put("companyCode", companyIssuer.getCompanyCode());
        data.put("lastUpdated", companyIssuer.getLastUpdated());

        List<LocalDate> dates = new ArrayList<>();
        List<Double> prices = new ArrayList<>();

        for (DayPrice historicalData : companyIssuer.getHistoricalData()) {
            dates.add(historicalData.getDate());
            prices.add(historicalData.getLastTransactionPrice());
        }

        data.put("dates", dates);
        data.put("prices", prices);
        data.put("id", companyIssuer.getId());
        companyData.add(data);

        model.addAttribute("companyData", companyData);
        return "companyIssuer";
    }

}
