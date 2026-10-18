package com.seol.tastediary.entry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "culture_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CultureEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @NotNull
    @PastOrPresent
    @Column(name = "experienced_on", nullable = false)
    private LocalDate experiencedOn;

    @Size(max = 10000)
    @Column(columnDefinition = "text")
    private String review;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    @Digits(integer = 1, fraction = 1)
    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 12, fraction = 0)
    @Column(name = "expense_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal expenseAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CultureEntry(String title, Category category, LocalDate experiencedOn,
                        String review, BigDecimal rating, BigDecimal expenseAmount) {
        update(title, category, experiencedOn, review, rating, expenseAmount);
    }

    public void update(String title, Category category, LocalDate experiencedOn,
                       String review, BigDecimal rating, BigDecimal expenseAmount) {
        if (rating != null && rating.remainder(new BigDecimal("0.5")).signum() != 0) {
            throw new IllegalArgumentException("평점은 0.5점 단위로 입력해야 합니다.");
        }
        this.title = title;
        this.category = category;
        this.experiencedOn = experiencedOn;
        this.review = review;
        this.rating = rating;
        this.expenseAmount = expenseAmount;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }
}
