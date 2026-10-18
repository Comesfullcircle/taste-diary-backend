package com.seol.tastediary.entry.domain;

import jakarta.persistence.EntityManager;
import com.seol.tastediary.member.domain.Member;
import com.seol.tastediary.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    private MemberRepository memberRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        memberId = memberRepository.saveAndFlush(new Member(
                "domain-test@example.com", "test-hash")).getId();
    }


    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("기록 저장 및 조회")
    void saveAndFind() {
        CultureEntry entry = new CultureEntry(memberId, "영화 관람", Category.MOVIE,
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
    @DisplayName("감상·평점 없이 무료 활동 저장")
    void saveWithoutReviewAndRating() {
        CultureEntry entry = new CultureEntry(memberId, "무료 전시", Category.EXHIBITION,
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
    @DisplayName("0.5점 단위가 아닌 평점 거부")
    void rejectInvalidRatingStep() {
        assertThatThrownBy(() -> new CultureEntry(memberId, "영화", Category.MOVIE,
                LocalDate.of(2026, 1, 1), null, new BigDecimal("4.3"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
