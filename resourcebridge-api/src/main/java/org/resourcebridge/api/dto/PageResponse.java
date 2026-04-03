package org.resourcebridge.api.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Consistent paginated response wrapper for all list endpoints.
 * Keeps the JSON contract stable regardless of Spring's internal Page structure.
 */
@Data
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> of(Page<T> springPage) {
        PageResponse<T> r = new PageResponse<>();
        r.setContent(springPage.getContent());
        r.setPage(springPage.getNumber());
        r.setSize(springPage.getSize());
        r.setTotalElements(springPage.getTotalElements());
        r.setTotalPages(springPage.getTotalPages());
        r.setFirst(springPage.isFirst());
        r.setLast(springPage.isLast());
        return r;
    }
}
