package com.builder;

import com.builder.processes.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@ManagedBean
@SessionScoped
public class MyDataset {

    private Part file;
    private Map<String, Integer> headers;
    private CSVParser fileParser;
    private String selectedType;
    private String value;
    private String selectedHeader;
    private Filters filters = new Filters();
    private Transformers transformers = new Transformers();
    private Counters counters = new Counters();
    private HashMap<String, AbstractProcess> processes = new HashMap<String, AbstractProcess>() {{
        put("filters",filters);
        put("transformers", transformers);
        put("counters", counters);
    }};
    private final List<String> filterTypeOptions = filters.getTypeOptions();

    public Part getFile() {
        return file;
    }

    public void setFile(Part file) {
        this.file = file;
    }

    public Filters getFilters() {
        return filters;
    }

    public void setFilters(Filters filters) {
        this.filters = filters;
    }

    public Set<String> getHeaders() {
        return headers.keySet();
    }

    public Map<String, Integer> getHeadersMap() {
        return headers;
    }

    @PostConstruct
    public void initializeFile() throws IOException {
        Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        fileParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
        headers = fileParser.getHeaderMap();
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.sendRedirect("http://localhost:8080/filters.xhtml");
    }

    public void addNewFilter(String processName) {
        this.processes.get(processName).add(selectedHeader, selectedType, value);
    }

    public void next(String processName) throws IOException {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.sendRedirect(this.processes.get(processName).nextPage());
    }

    public void previous(String processName) throws IOException {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.sendRedirect(this.processes.get(processName).previous());
    }

    public String getSelectedHeader() {
        return selectedHeader;
    }


    public void setSelectedHeader(String selectedHeader) {
        this.selectedHeader = selectedHeader;
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
