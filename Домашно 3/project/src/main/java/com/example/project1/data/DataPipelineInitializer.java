package com.example.project1.data;

import com.example.project1.data.pipeline.DataPipeline;
import com.example.project1.data.pipeline.impl.CorporationValidator;
import com.example.project1.data.pipeline.impl.PastDataRetriever;
import com.example.project1.data.pipeline.impl.FutureDataRetriever;
import com.example.project1.model.CorporationIssuer;
import com.example.project1.repository.CorporationIssuerRepository;
import com.example.project1.repository.CorporationDataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataPipelineInitializer {

    private final CorporationIssuerRepository issuerRepository;
    private final CorporationDataRepository dataRepository;

    @PostConstruct
    private void initializeDataPipeline() throws IOException, ParseException {
        long startTimeNano = System.nanoTime();

        DataPipeline<List<CorporationIssuer>> pipeline = new DataPipeline<>();
        pipeline.addFilter(new CorporationValidator(issuerRepository));
        pipeline.addFilter(new FutureDataRetriever(issuerRepository, dataRepository));
//        pipeline.addFilter(new PastDataRetriever(issuerRepository, dataRepository));

        pipeline.runFilter(null);

        long endTimeNano = System.nanoTime();
        long durationInMillis = (endTimeNano - startTimeNano) / 1_000_000;

        long hours = durationInMillis / 3_600_000;
        long minutes = (durationInMillis % 3_600_000) / 60_000;
        long seconds = (durationInMillis % 60_000) / 1_000;

        printExecutionTime(hours, minutes, seconds);
    }

    private void printExecutionTime(long hours, long minutes, long seconds) {
        System.out.printf("Total execution time for all filters: %02d hours, %02d minutes, %02d seconds%n", hours, minutes, seconds);
    }
}
