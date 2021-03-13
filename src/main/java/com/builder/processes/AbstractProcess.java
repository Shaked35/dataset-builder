package com.builder.processes;

import org.bson.Document;

import java.util.List;
import java.util.stream.Stream;

public interface AbstractProcess {
    void add(String header, String conditionType, String conditionValue);
    List<String> getTypeOptions();
    Document apply(Document row);
    String nextPage();
    String previous();
    List<TableRow> getTablesRows();
    void remove(TableRow row);


}
