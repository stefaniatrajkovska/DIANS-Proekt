package com.example.project1.web.controller;

import com.example.project1.model.CorporationIssuer;
import com.example.project1.model.CorporationData;
import com.example.project1.service.CorporationIssuerService;
import com.example.project1.service.AiService;
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
public class CorporationDataController {

    private final CorporationIssuerService corporationIssuerService;
    private final AiService aiService;

    // Метод за прикажување на сите компании
    @GetMapping("/corporations")
    public String getCompaniesPage(Model model) {
        try {
            model.addAttribute("corporations", corporationIssuerService.findAll());
            return "corporations-page";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to retrieve corporation data: " + e.getMessage());
            return "error-page";
        }
    }

    // Метод за прикажување на детали за конкретна компанија
    @GetMapping("/corporation")
    public String getCompanyPage(@RequestParam(name = "corporationId") Long companyId, Model model) {
        if (companyId == null || companyId <= 0) {
            model.addAttribute("error", "Invalid company ID.");
            return "error-page";
        }

        try {
            CorporationIssuer corporation = corporationIssuerService.findById(companyId);
            if (corporation == null) {
                model.addAttribute("error", "Corporation not found.");
                return "error-page";
            }

            Map<String, Object> data = new HashMap<>();
            data.put("corporationCode", corporation.getCorporationCode());
            data.put("dateLastUpdated", corporation.getDateLastUpdated());

            List<LocalDate> dates = new ArrayList<>();
            List<Double> prices = new ArrayList<>();

            for (CorporationData historicalData : corporation.getHistoricalRecords()) {
                dates.add(historicalData.getDate());
                prices.add(historicalData.getTransaction_price());
            }

            data.put("dates", dates);
            data.put("prices", prices);
            data.put("id", corporation.getId());

            model.addAttribute("corporationId", companyId);
            return "corporation-analysis";
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while retrieving corporation data: " + e.getMessage());
            return "error-page";
        }
    }
}
