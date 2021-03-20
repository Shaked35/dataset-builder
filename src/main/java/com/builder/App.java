package com.builder;

import com.builder.processes.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.bson.Document;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.builder.utils.Constants.*;
import static com.builder.utils.Utils.*;
import static com.sun.org.apache.xerces.internal.impl.XMLEntityManager.DEFAULT_BUFFER_SIZE;


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
    private List<String> tmpHeaders = new ArrayList<>();
    private Filters filters = new Filters();
    private Transformers transformers = new Transformers();
    private Counters counters = new Counters();
    private HashMap<String, AbstractProcess> processes = new HashMap<String, AbstractProcess>() {{
        put(FILTERS, filters);
        put(TRANSFORMERS, transformers);
        put(COUNTERS, counters);
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
        response.sendRedirect(WEB_PREFIX_URL+FILTERS+XHTML_SUFFIX);
    }

    public void addNewProcessValue(String processName) {
        System.out.println("Add new condition");
        if (processName.equals(COUNTERS)){
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
        if (processName.equals(FILTERS) && !headers.containsKey("date"))
            response.sendRedirect(WEB_PREFIX_URL+"counters_without_date"+XHTML_SUFFIX);
        else {
            response.sendRedirect(this.processes.get(processName).nextPage());
        }
    }

    public void previous(String processName) throws IOException {
        response.sendRedirect(this.processes.get(processName).previous());
    }

    public void apply() throws IOException {
        System.out.println("start file process");
        File tmpFile = File.createTempFile("tmp_file_" + UUID.randomUUID().toString(), ".csv");
        String finalFileName = "final_file_" + UUID.randomUUID().toString();
        File finalFile = File.createTempFile(finalFileName, ".csv");
        try {
            Writer writer = new BufferedWriter(new FileWriter(tmpFile));
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
            AtomicInteger counter = new AtomicInteger(0);
            List<String> headersToPrint = new ArrayList<>();
            first_iteration_process(csvPrinter, counter, headersToPrint);
            System.out.println("finished first iteration");
            close(writer, csvPrinter, counter);
            Reader reader = new InputStreamReader(new FileInputStream(tmpFile), StandardCharsets.UTF_8);
            fileParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            headers = fileParser.getHeaderMap();
            writer = new BufferedWriter(new FileWriter(finalFile));
            CSVPrinter finalCsvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
            second_iteration_process(counter, headersToPrint, finalCsvPrinter);
            System.out.println("finished second iteration");
            close(writer, finalCsvPrinter, counter);
            downloadFile(finalFile);
            response.sendRedirect(WEB_PREFIX_URL+"finished"+XHTML_SUFFIX);
        }finally{
            if (finalFile.delete() && tmpFile.delete()){
                System.out.println("All files deleted");
            }
        }
    }

    public void exit() throws IOException {
        totalClean();
        response.sendRedirect(WEB_PREFIX_URL+"newFile"+XHTML_SUFFIX);
    }

    private void totalClean() {
        file = null;
        headers = new HashMap<>();
        fileParser = null;
        selectedType = null;
        value = null;
        selectedHeader = null;
        selectedHeaderToRemove = null;
        tmpHeaders = new ArrayList<>();
        filters = new Filters();
        transformers = new Transformers();
        counters = new Counters();
        processes = new HashMap<String, AbstractProcess>() {{
            put(FILTERS, filters);
            put(TRANSFORMERS, transformers);
            put(COUNTERS, counters);
        }};
    }

    public void anotherOne() throws IOException {
        totalClean();
        response.sendRedirect(WEB_PREFIX_URL+FILTERS+XHTML_SUFFIX);
    }

    private void second_iteration_process(AtomicInteger counter, List<String> headersToPrint, CSVPrinter finalCsvPrinter) {
        streamFile(fileParser)
                .map(row-> this.processes.get(TRANSFORMERS).apply(row))
                .peek(initialized_headers(finalCsvPrinter, counter, headersToPrint))
                .forEach(write(finalCsvPrinter, headersToPrint));
    }

    private void first_iteration_process(CSVPrinter csvPrinter, AtomicInteger counter, List<String> headersToPrint) {
        streamFile(fileParser)
                .map(row-> this.processes.get(FILTERS).apply(row))
                .filter(Objects::nonNull)
                .sorted(sortByDate())
                .map(row-> this.processes.get(COUNTERS).apply(row))
                .peek(row-> ((Transformers) this.processes.get(TRANSFORMERS)).learn(row))
                .peek(initialized_headers(csvPrinter, counter, headersToPrint))
                .forEach(write(csvPrinter, headersToPrint));
    }

    private void close(Writer writer, CSVPrinter csvPrinter, AtomicInteger counter) throws IOException {
        csvPrinter.flush();
        writer.close();
        counter.set(0);
    }

    private Consumer<Document> write(CSVPrinter csvPrinter, List<String> headersToPrint) {
        return row-> {
            try {
                writeToFile(csvPrinter, headersToPrint, row);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private Consumer<Document> initialized_headers(CSVPrinter csvPrinter, AtomicInteger counter, List<String> headersToPrint) {
        return firstRow ->{
            try {
                addNewHeaders(counter, firstRow, csvPrinter, headersToPrint, headers);
            } catch (IOException e) {
                e.printStackTrace();
            }
            counter.getAndAdd(1);
        };
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

    public void downloadFile(File file) throws IOException {
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setContentType(CONTENT_TYPE);
        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
        BufferedInputStream input;
        BufferedOutputStream output = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
            output = new BufferedOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE);

            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null){
                output.flush();
            }
        }
    }
}
