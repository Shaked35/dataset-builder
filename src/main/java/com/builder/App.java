package com.builder;

import com.builder.processes.Statistics;
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
    private Statistics statistics = new Statistics();
    private HashMap<String, AbstractProcess> processes = new HashMap<String, AbstractProcess>() {{
        put(FILTERS, filters);
        put(TRANSFORMERS, transformers);
        put(Statistics, statistics);
    }};
    private File finalFile;

    /**
     * Get file from user
     * @return file
     */
    public Part getFile() {
        return file;
    }

    /**
     * Get file from user
     * @param file: uploaded file
     */
    public void setFile(Part file) {
        this.file = file;
    }

    /**
     * Get set list of headers
     * @return headers: csv headers
     */
    public Set<String> getHeaders() {
        return headers.keySet();
    }

    /**
     * This function save the csv from user and initialize the headers
     */
    @PostConstruct
    public void initializeFile() throws IOException {
        if (file != null) {
            try {
                if (file.getContentType().equals("text/csv")) {
                    Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                    fileParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
                    headers = fileParser.getHeaderMap();
                    HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                            .getExternalContext().getResponse();
                    response.sendRedirect(WEB_PREFIX_URL + FILTERS + XHTML_SUFFIX);
                }else{
                    throw new IllegalStateException("Illegal file type");
                }
            } catch (Exception e) {
                HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                        .getExternalContext().getResponse();
                response.sendRedirect(WEB_PREFIX_URL + "error" + XHTML_SUFFIX);
            }
        }
    }

    /**
     * Return to upload file page when we have an error.
     */
    public void backHome() throws IOException {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                .getExternalContext().getResponse();
        response.sendRedirect(WEB_PREFIX_URL + NEW_FILE + XHTML_SUFFIX);
    }

    /**
     * This function add the chosen value from user for the relevant process.
     * @param processName: process type: filter or transformer or statistics
     */
    public void addNewProcessValue(String processName) {
        System.out.println("Add new condition");
        if (processName.equals(Statistics)) {
            selectedHeader = String.join(",", this.tmpHeaders);
            this.tmpHeaders.clear();
        }
        this.processes.get(processName).add(selectedHeader, selectedType, value);
    }

    /**
     * Remove a row from process
     * @param processName: process type: filter or transformer or statistics
     * @param row:         row to remove from current table
     */
    public void removeProcessValue(String processName, TableRow row) {
        System.out.println("Remove condition");
        System.out.println("condition to remove: header=" + selectedHeader + ", type=" + selectedType + ", vale=" + value);
        this.processes.get(processName).remove(row);
    }

    /**
     * Go to the next page
     * @param processName: process type: filter or transformer or statistics
     */
    public void next(String processName) throws IOException {
        value = "";
        selectedHeader = "";
        selectedType = "";
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                .getExternalContext().getResponse();
        if (processName.equals(FILTERS) && !headers.containsKey("date"))

            response.sendRedirect(WEB_PREFIX_URL + "statisticsWithoutDate" + XHTML_SUFFIX);
        else {
            response.sendRedirect(this.processes.get(processName).nextPage());
        }
    }

    /**
     * Go to the previous page
     * @param processName: process type: filter or transformer or statistics
     */
    public void previous(String processName) throws IOException {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                .getExternalContext().getResponse();
        response.sendRedirect(this.processes.get(processName).previous());
    }

    /**
     * This function run the main process. After we got all of the values from the user we create the output file.
     */
    public void apply() throws IOException {
        System.out.println("start file process");
        File tmpFile = File.createTempFile("tmp_file_" + UUID.randomUUID().toString(), ".csv");
        String finalFileName = "final_file_" + UUID.randomUUID().toString();
        finalFile = File.createTempFile(finalFileName, ".csv");
        try {
            Writer writer = new BufferedWriter(new FileWriter(tmpFile));
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
            AtomicInteger counter = new AtomicInteger(0);
            List<String> headersToPrint = new ArrayList<>();
            firstIterationProcess(csvPrinter, counter, headersToPrint);
            System.out.println("finished first iteration");
            close(writer, csvPrinter, counter);
            headersToPrint = new ArrayList<>();
            Reader reader = new InputStreamReader(new FileInputStream(tmpFile), StandardCharsets.UTF_8);
            fileParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            headers = fileParser.getHeaderMap();
            writer = new BufferedWriter(new FileWriter(finalFile));
            CSVPrinter finalCsvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
            secondIterationProcess(counter, headersToPrint, finalCsvPrinter);
            System.out.println("finished second iteration");
            close(writer, finalCsvPrinter, counter);
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                    .getExternalContext().getResponse();
            response.sendRedirect(WEB_PREFIX_URL + "finished" + XHTML_SUFFIX);
        } finally {
            if (tmpFile.delete()) {
                System.out.println("All files deleted");
            }
        }
    }

    /**
     * Exist from website
     */
    public void exit() throws IOException {
        totalClean();
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                .getExternalContext().getResponse();
        response.sendRedirect(WEB_PREFIX_URL + "index" + XHTML_SUFFIX);
    }

    /**
     * Clean the previews values.
     */
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
        statistics = new Statistics();
        processes = new HashMap<String, AbstractProcess>() {{
            put(FILTERS, filters);
            put(TRANSFORMERS, transformers);
            put(Statistics, statistics);
        }};
    }

    /**
     * Go and build new dataset.
     */
    public void anotherOne() throws IOException {
        totalClean();
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                .getExternalContext().getResponse();
        response.sendRedirect(WEB_PREFIX_URL + NEW_FILE + XHTML_SUFFIX);
    }

    /**
     * Write the final file with the transformers.
     * @param counter:         number of rows
     * @param headersToPrint:  the final headers
     * @param finalCsvPrinter: final csv printer
     */
    private void secondIterationProcess(AtomicInteger counter, List<String> headersToPrint, CSVPrinter finalCsvPrinter) {
        streamFile(fileParser)
                .map(row -> this.processes.get(TRANSFORMERS).apply(row))
                .peek(initializedHeaders(finalCsvPrinter, counter, headersToPrint))
                .forEach(write(finalCsvPrinter, headersToPrint));
    }

    /**
     * Write the tmp file before calculate transformers.
     * @param counter:        number of rows
     * @param headersToPrint: the final headers
     */
    private void firstIterationProcess(CSVPrinter csvPrinter, AtomicInteger counter, List<String> headersToPrint) {
        streamFile(fileParser)
                .map(row -> this.processes.get(FILTERS).apply(row))
                .filter(Objects::nonNull)
                .sorted(sortByDate())
                .map(row -> this.processes.get(Statistics).apply(row))
                .peek(row -> ((Transformers) this.processes.get(TRANSFORMERS)).learn(row))
                .peek(initializedHeaders(csvPrinter, counter, headersToPrint))
                .forEach(write(csvPrinter, headersToPrint));
    }


    /**
     * Close resources.
     *
     * @param counter:    number of rows
     * @param csvPrinter: csv printer
     * @param writer:     csv writer
     */
    private void close(Writer writer, CSVPrinter csvPrinter, AtomicInteger counter) throws IOException {
        csvPrinter.flush();
        writer.close();
        counter.set(0);
    }

    /**
     * Write row of output file
     *
     * @param headersToPrint: output headers
     * @param csvPrinter:     csv printer
     */
    private Consumer<Document> write(CSVPrinter csvPrinter, List<String> headersToPrint) {
        return row -> {
            try {
                writeToFile(csvPrinter, headersToPrint, row);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    /**
     * Write the headers of the output file
     *
     * @param headersToPrint: output headers
     * @param csvPrinter:     csv printer
     * @param counter:        number of rows
     */
    private Consumer<Document> initializedHeaders(CSVPrinter csvPrinter, AtomicInteger counter, List<String> headersToPrint) {
        return firstRow -> {
            try {
                addNewHeaders(counter, firstRow, csvPrinter, headersToPrint, headers);
            } catch (IOException e) {
                e.printStackTrace();
            }
            counter.getAndAdd(1);
        };
    }


    /**
     * Add statics values to the current statistic
     */
    public void addToCurrentStatistic() {
        if (!this.tmpHeaders.contains(this.selectedHeader)) {
            tmpHeaders.add(this.selectedHeader);
        }
    }

    /**
     * Remove statics values to the current statistic
     */
    public void removeFromCurrentStatistic() {
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

    /**
     * Send the output file to the user.
     */
    public void downloadFile() throws IOException {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance()
                .getExternalContext().getResponse();
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setContentType(CONTENT_TYPE);
        response.setHeader("Content-Length", String.valueOf(this.finalFile.length()));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + this.finalFile.getName() + "\"");
        BufferedInputStream input;
        BufferedOutputStream output = null;
        try {
            input = new BufferedInputStream(new FileInputStream(this.finalFile), DEFAULT_BUFFER_SIZE);
            output = new BufferedOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE);

            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            FacesContext.getCurrentInstance().responseComplete();
        } catch (IOException e) {
            System.out.println("file warning");
            ;
        } finally {
            if (output != null) {
                output.flush();
            }
        }
    }
}
