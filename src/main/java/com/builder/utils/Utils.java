package com.builder.utils;

import com.builder.processes.TableRow;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.bson.Document;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class Utils {

    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Get enum name by value
     * @param value: target value
     * @param names: enum options
     * @return name: enum name
     */
    public static String getEnumByValue(String value, List<Enum> names) {
        return names.stream()
                .filter(d-> d.toString().equals(value))
                .map(Enum::name)
                .collect(Collectors.toList()).get(0);
    }

    /**
     * Find if the current row have the same values as the target row
     * @param row: target row
     * @return true if the target row not equals to the current row
     */
    public static <T> Predicate<HashMap<String, T>> filterCondition(TableRow row) {
        return condition -> {
            String conditionHeader = condition.keySet().toArray()[0].toString();
            String conditionFilterType = condition.values().toArray()[0].toString();
            return !(conditionHeader.equals(row.getHeader()) &&
                    conditionFilterType.equals(row.getProcessType()));
        };
    }

    public static List<TableRow> filterAndCollectTableRows(TableRow row, List<TableRow> rows) {
        return rows.stream().filter(
                condition -> !condition.equals(row)
        ).collect(Collectors.toList());
    }

    public static Stream<Document> streamFile(CSVParser fileParser) {
        return StreamSupport.stream(fileParser.spliterator(), false).map(record -> {
            Document document = new Document();
            document.putAll(record.toMap());
            return document;
        });
    }

    public static Comparator<? super Document> sortByDate() {
        return (d1, d2)->{
            LocalDate date1 = LocalDate.parse(d1.getString("date"), dateTimeFormatter);
            LocalDate date2 = LocalDate.parse(d2.getString("date"), dateTimeFormatter);
            return date1.compareTo(date2);
        };
    }

    public static void writeToFile(CSVPrinter writer, List<String> headers, Document row) throws IOException {
        List<String> rowToString = headers.stream()
                .map(head -> row.get(head) == null ? "nil" : String.valueOf(row.get(head)))
                .collect(toList());
        syncWrite(rowToString, writer);
    }

    private static synchronized void syncWrite(List<String> row, CSVPrinter actual) throws IOException {
        actual.printRecord(row);
    }

    public static void addNewHeaders(AtomicInteger counter, Document firstRow, CSVPrinter csvPrinter,
                               List<String> headersToPrint, Map<String, Integer> headers) throws IOException {
        if (counter.get() == 0){
            firstRow.keySet().iterator().forEachRemaining(head->{
                if (!headers.containsKey(head)){
                    headers.put(head, 1);
                }
            });
            headersToPrint.addAll(Arrays.stream(headers.keySet().toArray()).map(String::valueOf).collect(Collectors.toList()));
            System.out.println("The new headers: "+headersToPrint);
            csvPrinter.printRecord(headersToPrint);
        }
    }

}
