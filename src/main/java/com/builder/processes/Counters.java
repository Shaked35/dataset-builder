package com.builder.processes;

import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class Counters  implements AbstractProcess{
    @Override
    public void add(String header, String filterType, String filterValue) {

    }

    @Override
    public List<TableRow> getTablesRows() {
        return this.rows;
    }

    @Override
    public <T> List<HashMap<String, HashMap<T, String>>> get() {
        return null;
    }

    @Override
    public List<String> getTypeOptions() {
        return null;
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
}
