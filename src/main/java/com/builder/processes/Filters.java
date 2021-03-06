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
    private List<HashMap<String, HashMap<FilterType, String>>> filters;
    private List<TableRow> rows = new ArrayList<>();

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
        return "http://localhost:8080/counters.xhtml";
    }

    @Override
    public String previous() {
        return null;
    }

    @Override
    public List<TableRow> getTablesRows() {
        return this.rows;
    }

    @Override
    public void remove(TableRow row) {
        filters = filters.stream().filter(
                condition->{
                    String conditionHeader = condition.keySet().toArray()[0].toString();
                    HashMap<FilterType, String> values = (HashMap<FilterType, String>) condition.values().toArray()[0];
                    String conditionFilterType = values.keySet().toArray()[0].toString();
                    String conditionValue = values.values().toArray()[0].toString();
                    return !(conditionHeader.equals(row.getHeader()) && conditionValue.equals(row.getValue()) &&
                            conditionFilterType.equals(row.getProcessType()));
                }
        ).collect(Collectors.toList());
        rows = rows.stream().filter(
                condition-> !condition.equals(row)
        ).collect(Collectors.toList());
    }

    public void add(String header, String conditionType, String conditionValue) {
        HashMap<FilterType, String> newCondition = new HashMap<>();
        newCondition.put(FilterType.valueOf(Utils.getEnumByValue(conditionType, Arrays.asList(FilterType.values()))),
                conditionValue);
        HashMap<String, HashMap<FilterType, String>> newFilter = new HashMap<>();
        newFilter.put(header, newCondition);
        filters.add(newFilter);
        rows.add(new TableRow(header, conditionType, conditionValue));
    }

    public List<HashMap<String, HashMap<FilterType, String>>> get(){
        return this.filters;
    }


}
