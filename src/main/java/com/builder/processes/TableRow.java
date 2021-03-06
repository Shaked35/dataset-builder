package com.builder.processes;

public class TableRow {
    private final String header;
    private final String processType;
    private final String value;
    public TableRow(String header, String processType, String value){
        this.header = header;
        this.processType = processType;
        this.value = value;
    }
    public String getHeader(){
        return this.header;
    }

    public String getProcessType(){
        return this.processType;
    }
    public String getValue() {
        return this.value;
    }

    public boolean equals(TableRow other){
        return other.getHeader().equals(this.getHeader()) &&
                other.getValue().equals(this.getValue()) &&
                other.getProcessType().equals(this.getProcessType());
    }


}
