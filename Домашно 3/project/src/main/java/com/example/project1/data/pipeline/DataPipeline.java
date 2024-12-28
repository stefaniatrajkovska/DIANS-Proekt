package com.example.project1.data.pipeline;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataPipeline<T> {

    private final List<DataFilter<T>> filterList = new CopyOnWriteArrayList<>();

    public void addFilter(DataFilter<T> filter) {
        if (!filterList.contains(filter)) {
            filterList.add(filter);
        }
    }

    public T runFilter(T input) throws IOException, ParseException {
        for (DataFilter<T> filter : filterList) {
            logFilterExecution(filter);
            input = filter.execute(input);
        }
        return input;
    }

    public List<DataFilter<T>> getFilters() {
        return Collections.unmodifiableList(filterList);
    }

    private void logFilterExecution(DataFilter<T> filter) {
        System.out.println("Executing filter: " + filter.getClass().getSimpleName());
    }
}
