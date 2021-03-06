package com.builder.processes;

import com.builder.utils.Utils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Counters implements AbstractProcess {
    private List<HashMap<String, Counters.CounterFunction>> counters;
    private List<TableRow> rows = new ArrayList<>();

    private enum CounterFunction {
        AVG("Average"), SUM("Sum");

        private final String function;

        CounterFunction(String condition) {
            this.function = condition;
        }

        @Override
        public String toString() {
            return function;
        }
    }

    public Counters(){
        counters = new ArrayList<>();
    }

    @Override
    public void add(String header, String conditionType, String conditionValue) {
        HashMap<String, Counters.CounterFunction> newCounter = new HashMap<>();
        newCounter.put(header, Counters.CounterFunction.valueOf(Utils.getEnumByValue(conditionType,
                Arrays.asList(Counters.CounterFunction.values()))));
        counters.add(newCounter);
        rows.add(new TableRow(header, conditionType, ""));
    }

    @Override
    public List<TableRow> getTablesRows() {
        return this.rows;
    }

    @Override
    public void remove(TableRow row) {
        counters = counters.stream().filter(
                condition -> {
                    String conditionHeader = condition.keySet().toArray()[0].toString();
                    String conditionFilterType = condition.values().toArray()[0].toString();
                    return !(conditionHeader.equals(row.getHeader()) &&
                            conditionFilterType.equals(row.getProcessType()));
                }
        ).collect(Collectors.toList());
        rows = rows.stream().filter(
                condition -> !condition.equals(row)
        ).collect(Collectors.toList());
    }


    @Override
    public <T> List<HashMap<String, HashMap<T, String>>> get() {
        return null;
    }

    @Override
    public List<String> getTypeOptions() {
        return Arrays.stream(Counters.CounterFunction.values()).map(Counters.CounterFunction::toString).collect(Collectors.toList());
    }

    @Override
    public Stream<Document> apply() {
        return null;
    }

    @Override
    public String nextPage() {
        return "http://localhost:8080/transformers.xhtml";
    }

    @Override
    public String previous() {
        return "http://localhost:8080/filters.xhtml";
    }
}
