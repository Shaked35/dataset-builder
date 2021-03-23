package com.builder.processes;

import com.builder.utils.Utils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.builder.utils.Constants.*;
import static com.builder.utils.Utils.filterAndCollectTableRows;
import static com.builder.utils.Utils.filterCondition;

public class Transformers implements AbstractProcess {
    private List<TableRow> rows = new ArrayList<>();
    private List<HashMap<String, Encoder>> transformers;
    private final HashMap<String, HashMap<String, Double>> minMaxLearning = new HashMap<>();
    private final HashMap<String, List<String>> ordinalEncoderLearning = new HashMap<>();

    private enum Encoder {
        MIN_MAX_SCALE("MinMaxScale"), ORDINAL_ENCODERS("OrdinalEncoder");

        private final String encoder;

        Encoder(String condition) {
            this.encoder = condition;
        }

        @Override
        public String toString() {
            return encoder;
        }
    }

    public Transformers() {
        transformers = new ArrayList<>();
    }

    @Override
    public void add(String header, String conditionType, String conditionValue) {
        HashMap<String, Transformers.Encoder> newTransformer = new HashMap<>();
        newTransformer.put(header, Transformers.Encoder.valueOf(Utils.getEnumByValue(conditionType,
                Arrays.asList(Transformers.Encoder.values()))));
        transformers.add(newTransformer);
        rows.add(new TableRow(header, conditionType, ""));

    }

    @Override
    public List<TableRow> getTablesRows() {
        return this.rows;
    }

    @Override
    public void remove(TableRow row) {
        transformers = transformers.stream().filter(filterCondition(row)).collect(Collectors.toList());
        rows = filterAndCollectTableRows(row, this.rows);
    }


    @Override
    public List<String> getTypeOptions() {
        return Arrays.stream(Encoder.values()).map(Encoder::toString)
                .collect(Collectors.toList());
    }

    @Override
    public Document apply(Document row) {
        Document newDoc = new Document(row);
        ordinalEncoderLearning.forEach((header, positions) ->
                newDoc.put("category_" + header, positions.indexOf(String.valueOf(row.get(header)))));
        minMaxLearning.forEach((header, values) -> {
            Double min = values.get(MIN);
            Double max = values.get(MAX);
            newDoc.put(MIN + "_" + MAX + "_" + header, minMaxMethod(min, max, Double.valueOf((String) row.get(header))));
        });
        return newDoc;
    }

    public void learn(Document row) {
        transformers.forEach(transformer -> {
            String header = transformer.keySet().iterator().next();
            Encoder encoder = transformer.values().iterator().next();
            initializedEncoder(row, encoder, header);
        });
    }

    private void initializedEncoder(Document row, Encoder encoder, String header) {
        if (encoder == Encoder.MIN_MAX_SCALE) {
            minMaxLearning(row, header);
        } else {
            ordinalEncoderLearning(row, header);
        }
    }

    private void ordinalEncoderLearning(Document row, String header) {
        String currentValue = String.valueOf(row.get(header));
        if (ordinalEncoderLearning.containsKey(header)) {
            List<String> partition = ordinalEncoderLearning.get(header);
            if (!partition.contains(currentValue)) {
                ordinalEncoderLearning.get(header).add(currentValue);
            }
        } else {
            List<String> partition = new ArrayList<>();
            partition.add(currentValue);
            ordinalEncoderLearning.put(header, partition);
        }
    }

    private void minMaxLearning(Document row, String header) {
        Double currentValue = Double.valueOf((String) row.get(header));
        if (minMaxLearning.containsKey(header)) {
            Double min = minMaxLearning.get(header).get(MIN);
            Double max = minMaxLearning.get(header).get(MAX);
            if (currentValue < min) {
                minMaxLearning.get(header).put(MIN, currentValue);
            }
            if (currentValue < max) {
                minMaxLearning.get(header).put(MAX, currentValue);
            }
        } else {
            HashMap<String, Double> newPartition = new HashMap<>();
            newPartition.put(MIN, currentValue);
            newPartition.put(MAX, currentValue);
            minMaxLearning.put(header, newPartition);
        }
    }

    private static Double minMaxMethod(Double min, Double max, Double currentValue) {
        return (currentValue - min) / (max - min);
    }

    @Override
    public String nextPage() {
        return WEB_PREFIX_URL + "finished" + XHTML_SUFFIX;
    }

    @Override
    public String previous() {
        return WEB_PREFIX_URL + Statistics + XHTML_SUFFIX;
    }

}
