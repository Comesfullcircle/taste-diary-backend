package com.seol.tastediary.entry.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record EntryListResponse(List<EntryResponse> content, int page, int size,
                                long totalElements, int totalPages) {
    public static EntryListResponse from(Page<EntryResponse> entries) {
        return new EntryListResponse(entries.getContent(), entries.getNumber(), entries.getSize(),
                entries.getTotalElements(), entries.getTotalPages());
    }
}
