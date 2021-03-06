package com.builder.processes;

import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public interface AbstractProcess {
    void add(String header, String conditionType, String conditionValue);
    <T> List<HashMap<String, HashMap<T, String>>> get();
    List<String> getTypeOptions();
    Stream<Document> apply();
    String nextPage();
    String previous();
    List<TableRow> getTablesRows();
    void remove(TableRow row);


}
