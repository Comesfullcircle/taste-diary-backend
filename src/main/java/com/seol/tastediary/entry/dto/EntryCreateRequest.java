package com.seol.tastediary.entry.dto;

import com.seol.tastediary.entry.domain.Category;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntryCreateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.") String title,
        @NotNull(message = "카테고리는 필수입니다.") Category category,
        @NotNull(message = "경험한 날짜는 필수입니다.")
        @PastOrPresent(message = "경험한 날짜는 미래일 수 없습니다.") LocalDate experiencedOn,
        @Size(max = 10000, message = "감상은 10000자 이하여야 합니다.") String review,
        @DecimalMin(value = "0.0", message = "평점은 0점 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "평점은 5점 이하여야 합니다.")
        @Digits(integer = 1, fraction = 1, message = "평점은 소수 첫째 자리까지 입력할 수 있습니다.")
        BigDecimal rating,
        @NotNull(message = "지출 금액은 필수입니다.")
        @DecimalMin(value = "0", message = "지출 금액은 0원 이상이어야 합니다.")
        @Digits(integer = 12, fraction = 0, message = "지출 금액은 최대 12자리 정수여야 합니다.")
        BigDecimal expenseAmount
) {
    @AssertTrue(message = "평점은 0.5점 단위로 입력해야 합니다.")
    public boolean isRatingStepValid() {
        return rating == null || rating.remainder(new BigDecimal("0.5")).signum() == 0;
    }
}
