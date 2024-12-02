package com.example.project1.data;

import com.example.project1.data.pipeline.Pipe;
import com.example.project1.data.pipeline.impl.FOne;
import com.example.project1.data.pipeline.impl.FTwo;
import com.example.project1.data.pipeline.impl.FThree;
import com.example.project1.model.CompanyIssuer;
import com.example.project1.repository.CompanyIssuerRepository;
import com.example.project1.repository.DayPriceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializr {

    private final CompanyIssuerRepository companyIssuerRepository;
    private final DayPriceRepository dayPriceRepository;

    @PostConstruct
    private void initializeData() throws IOException, ParseException {
        long startTime = System.nanoTime();

        Pipe<List<CompanyIssuer>> pipe = new Pipe<>();
        pipe.addFilter(new FOne(companyIssuerRepository));
        pipe.addFilter(new FTwo(companyIssuerRepository, dayPriceRepository));
        pipe.addFilter(new FThree(companyIssuerRepository, dayPriceRepository));
        pipe.runFilter(null);

        long endTime = System.nanoTime();
        long durationInMillis = (endTime - startTime) / 1_000_000;

        long hours = durationInMillis / 3_600_000;
        long minutes = (durationInMillis % 3_600_000) / 60_000;
        long seconds = (durationInMillis % 60_000) / 1_000;

        System.out.printf("Total time for all filters to complete: %02d hours, %02d minutes, %02d seconds%n", hours, minutes, seconds);
    }

}
