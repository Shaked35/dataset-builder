package com.builder;

import com.builder.processes.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.bson.Document;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.builder.utils.Constants.WEB_PREFIX_URL;
import static com.builder.utils.Utils.*;


@ManagedBean
@SessionScoped
public class App {

    private Part file;
    private Map<String, Integer> headers;
    private CSVParser fileParser;
    private String selectedType;
    private String value;
    private String selectedHeader;
    private String selectedHeaderToRemove;
    private final List<String> tmpHeaders = new ArrayList<>();
    private final Filters filters = new Filters();
    private final Transformers transformers = new Transformers();
    private final Counters counters = new Counters();
    private final HashMap<String, AbstractProcess> processes = new HashMap<String, AbstractProcess>() {{
        put("filters", filters);
        put("transformers", transformers);
        put("counters", counters);
    }};

    public Part getFile() {
        return file;
    }

    public void setFile(Part file) {
        this.file = file;
    }

    public Set<String> getHeaders() {
        return headers.keySet();
    }

    @PostConstruct
    public void initializeFile() throws IOException {
        Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        fileParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
        headers = fileParser.getHeaderMap();
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.sendRedirect(WEB_PREFIX_URL+"filters.xhtml");
    }

    public void addNewProcessValue(String processName) {
        System.out.println("Add new condition");
        if (processName.equals("counters")){
            selectedHeader = String.join(",", this.tmpHeaders);
            this.tmpHeaders.clear();
        }
        this.processes.get(processName).add(selectedHeader, selectedType, value);
    }

    public void removeProcessValue(String processName, TableRow row) {
        System.out.println("Remove condition");
        System.out.println("condition to remove: header=" + selectedHeader + ", type=" + selectedType + ", vale=" + value);
        this.processes.get(processName).remove(row);
    }

    public void next(String processName) throws IOException {
        value = "";
        selectedHeader = "";
        selectedType = "";
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        if (processName.equals("filters") && !headers.containsKey("date"))
            response.sendRedirect(WEB_PREFIX_URL+"counters_without_date.xhtml");
        else {
            response.sendRedirect(this.processes.get(processName).nextPage());
        }
    }

    public void previous(String processName) throws IOException {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.sendRedirect(this.processes.get(processName).previous());
    }

    public void apply() throws IOException {
        File tmpFile = File.createTempFile("tmp_file_" + UUID.randomUUID().toString(), ".csv");
        File finalFile = File.createTempFile("final_file_" + UUID.randomUUID().toString(), ".csv");
        Writer writer = new BufferedWriter(new FileWriter(tmpFile));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
        AtomicInteger counter = new AtomicInteger(0);
        List<String> headersToPrint = new ArrayList<>();
        streamFile(fileParser)
                .map(row-> this.processes.get("filters").apply(row))
                .filter(Objects::nonNull)
                .sorted(sortByDate())
                .map(row-> this.processes.get("counters").apply(row))
                .peek(row-> ((Transformers) this.processes.get("transformers")).learn(row))
                .peek(firstRow ->{
                    try {
                        addNewHeaders(counter, firstRow, csvPrinter, headersToPrint, headers);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    counter.getAndAdd(1);
                })
                .forEach(row-> {
                    try {
                        writeToFile(csvPrinter, headersToPrint, row);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        csvPrinter.flush();
        writer.close();
//        return
//        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
//        response.sendRedirect(this.processes.get(processName).previous());
    }




    public void addToCurrentCounter() {
        if (!this.tmpHeaders.contains(this.selectedHeader)) {
            tmpHeaders.add(this.selectedHeader);
        }
    }

    public void removeFromCurrentCounter() {
        tmpHeaders.remove(this.selectedHeaderToRemove);
    }

    public List<String> getTmpHeaders() {
        return this.tmpHeaders;
    }

    public String getSelectedHeader() {
        return selectedHeader;
    }


    public void setSelectedHeader(String selectedHeader) {
        this.selectedHeader = selectedHeader;
    }

    public String getSelectedHeaderToRemove() {
        return selectedHeaderToRemove;
    }


    public void setSelectedHeaderToRemove(String selectedHeaderToRemove) {
        this.selectedHeaderToRemove = selectedHeaderToRemove;
    }

    public String getSelectedType() {
        return selectedType;
    }


    public void setSelectedType(String selectedType) {
        this.selectedType = selectedType;
    }


    public String getValue() {
        return value;
    }


    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getTypeOptions(String processName) {
        return this.processes.get(processName).getTypeOptions();
    }

    public List<TableRow> getAddedList(String processName) {
        return this.processes.get(processName).getTablesRows();
    }
}
