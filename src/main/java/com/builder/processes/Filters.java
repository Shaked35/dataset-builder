package com.builder.processes;


import com.builder.utils.Utils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Filters implements AbstractProcess{
    private final List<HashMap<String, HashMap<FilterType, String>>> filters;

    private enum FilterType {
        EQUAL_TO("equal to"), GREATER_THAN("grater than"), LESS_THAN("less than"),
        CONTAINS("contains"), DOESNT_CONTAINS("doesn't contains");

        private final String condition;

        FilterType(String condition) {
            this.condition = condition;
        }

        @Override
        public String toString() {
            return condition;
        }
    }


    public Filters() {
        filters = new ArrayList<>();
    }

    public List<String> getTypeOptions() {
        return Arrays.stream(FilterType.values()).map(FilterType::toString).collect(Collectors.toList());
    }

    @Override
    public Stream<Document> apply() {
        
        return null;
    }

    @Override
    public String nextPage() {
        return null;
    }

    @Override
    public String previous() {
        return null;
    }

    @Override
    public List<TableRow> getTablesRows() {
        return this.rows;
    }

    public void add(String header, String filterType, String filterValue) {
        HashMap<FilterType, String> newCondition = new HashMap<>();
        newCondition.put(FilterType.valueOf(Utils.getEnumByValue(filterType, Arrays.asList(FilterType.values()))),
                filterValue);
        HashMap<String, HashMap<FilterType, String>> newFilter = new HashMap<>();
        newFilter.put(header, newCondition);
        filters.add(newFilter);
        rows.add(new TableRow(header, filterType, filterValue));
    }

    public List<HashMap<String, HashMap<FilterType, String>>> get(){
        return this.filters;
    }


}
