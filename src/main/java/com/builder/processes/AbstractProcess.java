package com.builder.processes;

import org.bson.Document;

import java.util.List;

public interface AbstractProcess {
    /**
     * Add new value in the relevant process
     *
     * @param header:         The name of the header that the client want.
     * @param conditionType:  Chosen condition by client.
     * @param conditionValue: Target value.
     */
    void add(String header, String conditionType, String conditionValue);

    /**
     * Get list of option type for each process
     */
    List<String> getTypeOptions();

    /**
     * Return the row with process values.
     *
     * @param row: The current row from file.
     */
    Document apply(Document row);

    /**
     * Return the name of the next page in flow.
     */
    String nextPage();

    /**
     * Return the name of the previous page in flow.
     */
    String previous();

    /**
     * Get all chosen values in process as list of table rows.
     */
    List<TableRow> getTablesRows();

    /**
     * Remove specific row.
     *
     * @param row: The row that the client want to remove.
     */
    void remove(TableRow row);


}
