package com.hhm.dto;

import com.hhm.model.Patient;

import java.util.List;

public class PatientPage {

    private final List<Patient> content;
    private final long totalElements;
    private final int totalPages;
    private final int pageNumber;
    private final int pageSize;

    public PatientPage(org.springframework.data.domain.Page<Patient> page) {
        this.content = page.getContent();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
    }

    public List<Patient> getContent() { return content; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getPageNumber() { return pageNumber; }
    public int getPageSize() { return pageSize; }
}
