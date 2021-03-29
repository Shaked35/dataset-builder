package com.builder.processes;


import com.builder.utils.Utils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.builder.utils.Constants.*;
import static com.builder.utils.Utils.filterAndCollectTableRows;

public class Filters implements AbstractProcess {
    private List<HashMap<String, HashMap<FilterType, String>>> filters;
    private List<TableRow> rows = new ArrayList<>();

    private enum FilterType {
        EQUAL_TO("equal to"), GREATER_THAN("grater than"), LESS_THAN("less than"),
        CONTAINS("contains"), DOESNT_CONTAINS("doesn't contain");

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
    public Document apply(Document row) {
        AtomicBoolean shouldBeRemoved = new AtomicBoolean(false);
        filters.forEach(filter -> {
            if (row.containsKey(filter.keySet().iterator().next())) {
                HashMap<FilterType, String> condition = filter.values().iterator().next();
                if (applyCondition(condition, String.valueOf(row.get(filter.keySet().iterator().next())))) {
                    shouldBeRemoved.set(true);
                }
            }
        });
        if (shouldBeRemoved.get()) {
            return null;
        } else {
            return new Document(row);
        }

    }

    @Override
    public String nextPage() {
        return WEB_PREFIX_URL + Statistics + XHTML_SUFFIX;
    }

    @Override
    public String previous() {
        return WEB_PREFIX_URL + "index" + XHTML_SUFFIX;
    }

    @Override
    public List<TableRow> getTablesRows() {
        return this.rows;
    }

    @Override
    public void remove(TableRow row) {
        filters = filters.stream().filter(
                condition -> {
                    String conditionHeader = condition.keySet().toArray()[0].toString();
                    HashMap<FilterType, String> values = condition.values().iterator().next();
                    String conditionFilterType = values.keySet().iterator().next().condition;
                    String conditionValue = values.values().toArray()[0].toString();
                    return !(conditionHeader.equals(row.getHeader()) && conditionValue.equals(row.getValue()) &&
                            conditionFilterType.equals(row.getProcessType()));
                }
        ).collect(Collectors.toList());
        rows = filterAndCollectTableRows(row, this.rows);
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

    private boolean applyCondition(HashMap<FilterType, String> condition, String field) {
        try {
            switch (condition.keySet().iterator().next()) {
                case GREATER_THAN:
                    if (!condition.values().iterator().next().equals("")) {
                        double value = Double.parseDouble(field);
                        double greaterThan = Double.parseDouble(condition.values().iterator().next());
                        return value < greaterThan;
                    }
                case LESS_THAN:
                    if (!condition.values().iterator().next().equals("")) {
                        double value = Double.parseDouble(field);
                        double lessThan = Double.parseDouble(condition.values().iterator().next());
                        return value > lessThan;
                    }
                case EQUAL_TO:
                    return !condition.values().iterator().next().equals(field);
                case CONTAINS:
                    return !condition.values().iterator().next().contains(field);
                case DOESNT_CONTAINS:
                    return condition.values().iterator().next().contains(field);

            }
            return false;
        } catch (java.lang.NumberFormatException e) {
            return false;
        }
    }
}
