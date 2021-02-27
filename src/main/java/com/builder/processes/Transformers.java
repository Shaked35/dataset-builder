package com.builder.processes;

import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class Transformers implements AbstractProcess {
    @Override
    public void add(String header, String filterType, String filterValue) {

    }

    @Override
    public <T> List<HashMap<String, HashMap<T, String>>> get() {
        return null;
    }

    @Override
    public List<TableRow> getTablesRows() {
        return this.rows;
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

    private enum TransformerType {
        ONE_HOT("one hot"), MIN_MAX_SCALE("min max scale"), LESS_THAN("less than"),
        CONTAINS("contains"), DOESNT_CONTAINS("doesn't contains");

        private final String transformValue;

        TransformerType(String transformValue) {
            this.transformValue = transformValue;
        }

        @Override
        public String toString() {
            return transformValue;
        }
    }
}
