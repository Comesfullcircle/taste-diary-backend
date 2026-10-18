package com.seol.tastediary.entry.domain;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CultureEntryPersistenceTests {

    @Autowired
    private EntityManager entityManager;

    @Test
    void 기록을_저장하고_다시_조회한다() {
        CultureEntry entry = new CultureEntry("영화 관람", Category.MOVIE,
                LocalDate.of(2026, 1, 1), "여운이 남았다", new BigDecimal("4.5"),
                new BigDecimal("15000"));

        entityManager.persist(entry);
        entityManager.flush();
        entityManager.clear();

        CultureEntry found = entityManager.find(CultureEntry.class, entry.getId());
        assertThat(found.getTitle()).isEqualTo("영화 관람");
        assertThat(found.getCategory()).isEqualTo(Category.MOVIE);
        assertThat(found.getExperiencedOn()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(found.getReview()).isEqualTo("여운이 남았다");
        assertThat(found.getRating()).isEqualByComparingTo("4.5");
        assertThat(found.getExpenseAmount()).isEqualByComparingTo("15000");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
    }

    @Test
    void 감상과_평점_없이_무료_활동을_기록할_수_있다() {
        CultureEntry entry = new CultureEntry("무료 전시", Category.EXHIBITION,
                LocalDate.of(2026, 1, 1), null, null, BigDecimal.ZERO);

        entityManager.persist(entry);
        entityManager.flush();
        entityManager.clear();

        CultureEntry found = entityManager.find(CultureEntry.class, entry.getId());
        assertThat(found.getReview()).isNull();
        assertThat(found.getRating()).isNull();
        assertThat(found.getExpenseAmount()).isEqualByComparingTo("0");
    }

    @Test
    void 평점은_반점_단위로만_입력한다() {
        assertThatThrownBy(() -> new CultureEntry("영화", Category.MOVIE,
                LocalDate.of(2026, 1, 1), null, new BigDecimal("4.3"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
