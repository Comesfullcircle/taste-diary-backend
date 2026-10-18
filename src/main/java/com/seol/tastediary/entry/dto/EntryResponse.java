package com.seol.tastediary.entry.dto;

import com.seol.tastediary.entry.domain.Category;
import com.seol.tastediary.entry.domain.CultureEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EntryResponse(Long id, String title, Category category, LocalDate experiencedOn,
                            String review, BigDecimal rating, BigDecimal expenseAmount,
                            Instant createdAt, Instant updatedAt) {
    public static EntryResponse from(CultureEntry entry) {
        return new EntryResponse(entry.getId(), entry.getTitle(), entry.getCategory(),
                entry.getExperiencedOn(), entry.getReview(), entry.getRating(),
                entry.getExpenseAmount(), entry.getCreatedAt(), entry.getUpdatedAt());
    }
}
