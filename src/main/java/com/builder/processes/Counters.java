package com.builder.processes;

import com.builder.utils.Utils;
import org.bson.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.builder.utils.Constants.*;
import static com.builder.utils.Constants.XHTML_SUFFIX;
import static com.builder.utils.Utils.dateTimeFormatter;
import static com.builder.utils.Utils.filterAndCollectTableRows;

public class Counters implements AbstractProcess {
    private List<HashMap<String, HashMap<Counters.CounterFunction, String>>> counters;
    private HashMap<LocalDate, HashMap<String, HashMap<String, Double>>> countersValuesLast7Days = new HashMap<>();
    private HashMap<LocalDate, HashMap<String, HashMap<String, Double>>> countersValuesLast14Days = new HashMap<>();
    private final HashMap<String, HashMap<String, Double>> currentDateData = new HashMap<>();
    private final HashMap<String, HashMap<String, Double>> tmpPartitionValues = new HashMap<>();
    private LocalDate currentDate;
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

    public Counters() {
        counters = new ArrayList<>();
    }

    @Override
    public void add(String header, String conditionType, String conditionValue) {
        HashMap<Counters.CounterFunction, String> newCondition = new HashMap<>();
        newCondition.put(Counters.CounterFunction.valueOf(Utils.getEnumByValue(conditionType,
                Arrays.asList(Counters.CounterFunction.values()))),
                conditionValue);
        HashMap<String, HashMap<Counters.CounterFunction, String>> newCounter = new HashMap<>();
        newCounter.put(header, newCondition);
        counters.add(newCounter);
        rows.add(new TableRow(header, conditionType, conditionValue));
    }

    @Override
    public List<TableRow> getTablesRows() {
        return this.rows;
    }

    @Override
    public void remove(TableRow row) {
        counters = counters.stream().filter(
                condition -> {
                    String conditionHeader = condition.keySet().iterator().next();
                    HashMap<Counters.CounterFunction, String> values = condition.values().iterator().next();
                    String conditionCounterType = values.keySet().iterator().next().function;
                    String conditionValue = values.values().iterator().next();
                    return !(conditionHeader.equals(row.getHeader()) && conditionValue.equals(row.getValue()) &&
                            conditionCounterType.equals(row.getProcessType()));
                }
        ).collect(Collectors.toList());
        rows = filterAndCollectTableRows(row, this.rows);
    }

    @Override
    public List<String> getTypeOptions() {
        return Arrays.stream(Counters.CounterFunction.values()).map(Counters.CounterFunction::toString).collect(Collectors.toList());
    }

    @Override
    public Document apply(Document row) {
        if (!counters.isEmpty()) {
            LocalDate rowDate = LocalDate.parse(row.getString("date"), dateTimeFormatter);
            if (!rowDate.equals(currentDate)) {
                initializedNewDate(rowDate);
                currentDate = rowDate;
                tmpPartitionValues.clear();
            }
            updatePartition(row);
            return addPartitionsValues(row);
        }
        return new Document(row);
    }

    private Document addPartitionsValues(Document row) {
        Document newDocument = new Document(row);
        counters.forEach(partition -> {
            PartitionBuilder partitionBuilder = new PartitionBuilder(row, partition).invoke();
            Counters.CounterFunction function = partition.values().iterator().next().keySet().iterator().next();
            if (!tmpPartitionValues.containsKey(partitionBuilder.newPartition) ||
                    !tmpPartitionValues.get(partitionBuilder.newPartition).containsKey(
                            partitionBuilder.currentPartitionValues + "-" + function + LAST_7)) {
                calculateCurrentPartition(partitionBuilder, function);
            }
            HashMap<String, Double> optionalValues = tmpPartitionValues.get(partitionBuilder.newPartition);
            newDocument.put(partitionBuilder.newPartition + "-" + function + LAST_7,
                    optionalValues.get(partitionBuilder.currentPartitionValues + "-" + function + LAST_7));
            newDocument.put(partitionBuilder.newPartition + "-" + function + LAST_14,
                    optionalValues.get(partitionBuilder.currentPartitionValues + "-" + function + LAST_14));

        });
        return newDocument;
    }

    private void calculateCurrentPartition(PartitionBuilder partitionBuilder, Counters.CounterFunction function) {
        List<Double> last7DaysValues = getLstDaysValues(partitionBuilder, countersValuesLast7Days, function);
        List<Double> last14DaysValues = getLstDaysValues(partitionBuilder, countersValuesLast14Days, function);
        initializedNewPartitionValue(partitionBuilder, function, last7DaysValues, last14DaysValues);
    }

    private void initializedNewPartitionValue(PartitionBuilder partitionBuilder, CounterFunction function, List<Double> last7DaysValues, List<Double> last14DaysValues) {
        double days7;
        double days14;
        if (function.equals(CounterFunction.SUM)) {
            days7 = last7DaysValues.stream().mapToDouble(Double::doubleValue).sum();
            days14 = last14DaysValues.stream().mapToDouble(Double::doubleValue).sum();
        } else {
            days7 = last7DaysValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            days14 = last14DaysValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        HashMap<String, Double> tmpDays = new HashMap<>();
        if (tmpPartitionValues.containsKey(partitionBuilder.newPartition)) {
            tmpDays = tmpPartitionValues.get(partitionBuilder.newPartition);
        }
        tmpDays.put(partitionBuilder.currentPartitionValues + "-" + function + LAST_7, days7);
        tmpDays.put(partitionBuilder.currentPartitionValues + "-" + function + LAST_14, days14);
        tmpPartitionValues.put(partitionBuilder.newPartition, tmpDays);
    }

    private List<Double> getLstDaysValues(PartitionBuilder partitionBuilder,
                                          HashMap<LocalDate, HashMap<String, HashMap<String, Double>>> countersValuesLastDays,
                                          Counters.CounterFunction function) {
        List<Double> lastDaysValues = new ArrayList<>();
        countersValuesLastDays.forEach((date, partition) -> {
            HashMap<String, Double> datePartition = partition.get(partitionBuilder.newPartition);
            if (datePartition.containsKey(partitionBuilder.currentPartitionValues + "-" + function)) {
                lastDaysValues.add(datePartition.get(partitionBuilder.currentPartitionValues + "-" + function));
            }
        });
        return lastDaysValues;
    }

    private void initializedNewDate(LocalDate rowDate) {
        countersValuesLast7Days = filterLastDays(rowDate.minusDays(7), new HashMap<>(countersValuesLast7Days));
        countersValuesLast14Days = filterLastDays(rowDate.minusDays(14), new HashMap<>(countersValuesLast14Days));
        if (currentDate != null) {
            countersValuesLast7Days.put(currentDate, currentDateData);
            countersValuesLast14Days.put(currentDate, currentDateData);
        }
        currentDateData.clear();

    }

    private void updatePartition(Document row) {
        counters.forEach(partition -> {
            PartitionBuilder partitionBuilder = new PartitionBuilder(row, partition).invoke();
            String currentPartitionValues = partitionBuilder.getCurrentPartitionValues();
            Double currentValue = partitionBuilder.getCurrentValue();
            String newPartition = partitionBuilder.getNewPartition();
            Counters.CounterFunction function = partition.values().iterator().next().keySet().iterator().next();
            Double lastValue = 0.0;
            if (currentDateData.containsKey(newPartition)) {
                HashMap<String, Double> optionalValues = currentDateData.get(newPartition);
                if (optionalValues.containsKey(currentPartitionValues + "-" + function)) {
                    lastValue = optionalValues.get(currentPartitionValues + "-" + function);
                }
                currentDateData.get(newPartition).put(currentPartitionValues + "-" + function, lastValue + currentValue);
            } else {
                HashMap<String, Double> tmpValues = new HashMap<>();
                tmpValues.put(currentPartitionValues + "-" + function, currentValue);
                currentDateData.put(newPartition, tmpValues);
            }
        });
    }

    private HashMap<LocalDate, HashMap<String, HashMap<String, Double>>> filterLastDays(LocalDate minDate,
                                                                                        HashMap<LocalDate, HashMap<String, HashMap<String, Double>>> currentData) {
        HashMap<LocalDate, HashMap<String, HashMap<String, Double>>> tmpDays = new HashMap<>();
        currentData.forEach((date, partitions) -> {
            if (date.compareTo(minDate) > -1) {
                tmpDays.put(date, partitions);
            }
        });
        return tmpDays;
    }


    @Override
    public String nextPage() {
        return WEB_PREFIX_URL + TRANSFORMERS + XHTML_SUFFIX;
    }

    @Override
    public String previous() {
        return WEB_PREFIX_URL + FILTERS + XHTML_SUFFIX;
    }

    private static class PartitionBuilder {
        private final Document row;
        private final HashMap<String, HashMap<CounterFunction, String>> partition;
        private String currentPartitionValues;
        private Double currentValue;
        private String newPartition;

        public PartitionBuilder(Document row, HashMap<String, HashMap<CounterFunction, String>> partition) {
            this.row = row;
            this.partition = partition;
        }

        public String getCurrentPartitionValues() {
            return currentPartitionValues;
        }

        public Double getCurrentValue() {
            return currentValue;
        }

        public String getNewPartition() {
            return newPartition;
        }

        public PartitionBuilder invoke() {
            List<String> headers = Arrays.asList(partition.keySet().iterator().next().split(","));
            currentPartitionValues = headers.stream()
                    .map(header -> String.valueOf(row.get(header)))
                    .collect(Collectors.joining("_"));
            String targetHeader = partition.values().iterator().next().values().iterator().next();
            currentPartitionValues += "-" + targetHeader;
            currentValue = Double.valueOf((String) row.get(targetHeader));
            newPartition = String.join("_", headers) + "-" + targetHeader;
            return this;
        }
    }
}
