package com.builder;

import com.builder.processes.Counters;
import com.builder.processes.Transformers;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.bson.Document;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.builder.utils.Utils.*;

public class actionsTests {

    @Test
    @Ignore
    public void filterTest() {
        List<Object> optionalValues = Arrays.asList("2", "2.3", "23434234", "sdf", 234, 1.1, true);
        double greaterThan = 2.2;
        optionalValues.forEach(o -> {
            try {
                String valueString = String.valueOf(o);
                double value = Double.parseDouble(valueString);
                if (value < greaterThan) {
                    System.out.println(greaterThan + " is greaterThan " + value);
                }
            } catch (java.lang.NumberFormatException e) {
                System.out.println(e);
            }
        });
    }

    @Test
    @Ignore
    public void dateTest() {
        List<String> optionalValues = Arrays.asList("2020-01-02", "2020-01-10", "2020-01-05", "2020-01-22", "2020-01-03");
        List<Document> s = optionalValues.stream().map(d -> {
            Document d1 = new Document();
            d1.put("date", d);
            return d1;
        }).sorted(sortByDate()).collect(Collectors.toList());
        System.out.println(s);
    }

    @Test
    @Ignore
    public void counterIntegrativeTest() throws IOException {
        Counters counters = new Counters();
//        counters.add("provider,position", "Sum", "sales");
        counters.add("provider,position", "Sum", "outbounds");
        counters.add("provider,position", "Average", "outbounds");
        File file = new File("outbound_test.csv");
        InputStream inputFile = new FileInputStream(file);
        Reader reader = new InputStreamReader(inputFile, StandardCharsets.UTF_8);
        CSVParser fileParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
        streamFile(fileParser)
                .sorted(sortByDate())
                .map(counters::apply)
                .forEach(d -> {
                    System.out.println(d);
                });
        reader.close();
//        counters.add("provider,position", "Average", "sales");
//        file = new File("integrative_test.csv");
//        inputFile = new FileInputStream(file);
//        reader = new InputStreamReader(inputFile, StandardCharsets.UTF_8);
//        fileParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
//        streamFile(fileParser)
//                .sorted(sortByDate())
//                .map(counters::apply)
//                .forEach(d -> {
//                    System.out.println(d);
//                });
//        reader.close();
    }

    @Test
    @Ignore
    public void integrativeTest() throws IOException {
        Counters counters = new Counters();
        counters.add("provider,position", "Sum", "outbounds");
        counters.add("provider,position", "Average", "outbounds");
        Transformers transformers = new Transformers();
        transformers.add("provider", "OrdinalEncoder", "");
        File file = new File("outbound_test.csv");
        InputStream inputFile = new FileInputStream(file);
        Reader reader = new InputStreamReader(inputFile, StandardCharsets.UTF_8);
        CSVParser fileParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
        Map<String, Integer> headers = fileParser.getHeaderMap();
        File tmpFile = File.createTempFile("tmp_file_" + UUID.randomUUID().toString(), ".csv");
        Writer writer = new BufferedWriter(new FileWriter(tmpFile));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
        AtomicInteger counter = new AtomicInteger(0);
        List<String> headersToPrint = new ArrayList<>();
        streamFile(fileParser)
                .filter(Objects::nonNull)
                .sorted(sortByDate())
                .map(counters::apply)
                .forEach(row -> {
                    transformers.learn(row);
                    try {
                        addNewHeaders(counter, row, csvPrinter, headersToPrint, headers);
                        writeToFile(csvPrinter, headersToPrint, row);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    counter.getAndAdd(1);
                });
        csvPrinter.flush();
        writer.close();
    }


}
