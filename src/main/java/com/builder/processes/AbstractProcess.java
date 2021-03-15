package com.builder.processes;

import org.bson.Document;

import java.util.List;

public interface AbstractProcess {
    void add(String header, String conditionType, String conditionValue);
    List<String> getTypeOptions();
    Document apply(Document row);
    String nextPage();
    String previous();
    List<TableRow> getTablesRows();
    void remove(TableRow row);


}
