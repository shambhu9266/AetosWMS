package com.example.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PaginationService {

    // Default page size for different types of data
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DASHBOARD_PAGE_SIZE = 10;
    private static final int LARGE_PAGE_SIZE = 50;

    public Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        // Validate and set default values
        if (page < 0) page = 0;
        if (size <= 0) size = DEFAULT_PAGE_SIZE;
        if (size > 100) size = 100; // Maximum page size limit
        
        // Create sort object
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        
        return PageRequest.of(page, size, sort);
    }

    public Pageable createDashboardPageable(int page) {
        return PageRequest.of(page, DASHBOARD_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Pageable createLargePageable(int page) {
        return PageRequest.of(page, LARGE_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Pageable createDefaultPageable(int page) {
        return PageRequest.of(page, DEFAULT_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // Helper method to create response with pagination metadata
    public <T> Object createPaginatedResponse(Page<T> page) {
        return new Object() {
            public final Object content = page.getContent();
            public final int currentPage = page.getNumber();
            public final int totalPages = page.getTotalPages();
            public final long totalElements = page.getTotalElements();
            public final int pageSize = page.getSize();
            public final boolean hasNext = page.hasNext();
            public final boolean hasPrevious = page.hasPrevious();
            public final boolean isFirst = page.isFirst();
            public final boolean isLast = page.isLast();
        };
    }
}
