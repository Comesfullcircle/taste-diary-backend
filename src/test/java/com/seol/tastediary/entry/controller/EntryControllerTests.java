package com.seol.tastediary.entry.controller;

import com.seol.tastediary.entry.repository.EntryRepository;
import com.seol.tastediary.entry.domain.Category;
import com.seol.tastediary.entry.domain.CultureEntry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EntryControllerTests {
    private static final String VALID_REQUEST = """
            {
              "title": "영화 관람",
              "category": "MOVIE",
              "experiencedOn": "2026-01-01",
              "review": "여운이 남았다",
              "rating": 4.5,
              "expenseAmount": 15000
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntryRepository entryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("작성 후 Location으로 상세 조회")
    void createAndFind() throws Exception {
        String location = mockMvc.perform(post("/api/entries")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn().getResponse().getHeader("Location");

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("영화 관람"))
                .andExpect(jsonPath("$.category").value("MOVIE"))
                .andExpect(jsonPath("$.experiencedOn").value("2026-01-01"))
                .andExpect(jsonPath("$.review").value("여운이 남았다"))
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.expenseAmount").value(15000));
    }

    @Test
    @DisplayName("감상·평점 없이 무료 활동 작성")
    void createWithoutReviewAndRating() throws Exception {
        mockMvc.perform(post("/api/entries").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"무료 전시", "category":"EXHIBITION",
                         "experiencedOn":"2026-01-01", "expenseAmount":0}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseAmount").value(0));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    @DisplayName("입력 검증 실패 시 400 응답 및 저장 방지")
    void rejectInvalidInput(String request) throws Exception {
        long countBefore = entryRepository.count();

        mockMvc.perform(post("/api/entries").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors").isNotEmpty());

        assertThat(entryRepository.count()).isEqualTo(countBefore);
    }

    static Stream<String> invalidRequests() {
        return Stream.of(
                VALID_REQUEST.replace("영화 관람", "   "),
                VALID_REQUEST.replace("영화 관람", "가".repeat(201)),
                VALID_REQUEST.replace("\"MOVIE\"", "null"),
                VALID_REQUEST.replace("\"2026-01-01\"", "null"),
                VALID_REQUEST.replace("2026-01-01", LocalDate.now().plusDays(1).toString()),
                VALID_REQUEST.replace("여운이 남았다", "가".repeat(10001)),
                VALID_REQUEST.replace("4.5", "-0.5"),
                VALID_REQUEST.replace("4.5", "5.5"),
                VALID_REQUEST.replace("4.5", "4.3"),
                VALID_REQUEST.replace("15000", "null"),
                VALID_REQUEST.replace("15000", "-1"),
                VALID_REQUEST.replace("15000", "1.5"),
                VALID_REQUEST.replace("15000", "1000000000000")
        );
    }

    @Test
    @DisplayName("평점 단위 오류의 필드명 확인")
    void returnRatingFieldError() throws Exception {
        mockMvc.perform(post("/api/entries").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST.replace("4.5", "4.3")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("rating")));
    }

    @ParameterizedTest
    @MethodSource("unreadableRequests")
    @DisplayName("요청 형식 오류 시 400 응답")
    void rejectUnreadableRequest(String request) throws Exception {
        mockMvc.perform(post("/api/entries").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    static Stream<String> unreadableRequests() {
        return Stream.of("{", "", VALID_REQUEST.replace("MOVIE", "UNKNOWN"),
                VALID_REQUEST.replace("2026-01-01", "2026-99-99"));
    }

    @Test
    @DisplayName("없는 기록 조회 시 404 응답")
    void returnNotFound() throws Exception {
        mockMvc.perform(get("/api/entries/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTRY_NOT_FOUND"));
    }

    @Test
    @DisplayName("숫자가 아닌 ID 요청 시 400 응답")
    void rejectInvalidId() throws Exception {
        mockMvc.perform(get("/api/entries/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("날짜·ID 내림차순 목록과 페이지 정보")
    void findPage() throws Exception {
        entryRepository.deleteAll();
        saveEntry("이전 기록", LocalDate.of(2026, 1, 1));
        saveEntry("같은 날짜 먼저", LocalDate.of(2026, 1, 2));
        saveEntry("같은 날짜 나중", LocalDate.of(2026, 1, 2));
        entityManager.clear();

        mockMvc.perform(get("/api/entries").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("같은 날짜 나중"))
                .andExpect(jsonPath("$.content[1].title").value("같은 날짜 먼저"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/entries").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("이전 기록"));

        mockMvc.perform(get("/api/entries").param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("빈 목록과 기본 페이지 설정")
    void findEmptyPage() throws Exception {
        entryRepository.deleteAll();

        mockMvc.perform(get("/api/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,-1", "0,101", "abc,20", "0,abc"})
    @DisplayName("잘못된 페이지 요청 시 400 응답")
    void rejectInvalidPage(String page, String size) throws Exception {
        mockMvc.perform(get("/api/entries").param("page", page).param("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isNotEmpty());
    }

    @Test
    @DisplayName("기록 수정 및 생성·수정 시각 확인")
    void updateEntry() throws Exception {
        CultureEntry entry = saveEntry("기존 제목", LocalDate.of(2025, 12, 1));
        Long id = entry.getId();
        entityManager.clear();
        Instant createdAt = entryRepository.findById(id).orElseThrow().getCreatedAt();
        entityManager.clear();

        String body = mockMvc.perform(put("/api/entries/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("영화 관람"))
                .andReturn().getResponse().getContentAsString();

        entityManager.clear();
        CultureEntry updated = entryRepository.findById(id).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("영화 관람");
        assertThat(updated.getCategory()).isEqualTo(Category.MOVIE);
        assertThat(updated.getExperiencedOn()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(updated.getReview()).isEqualTo("여운이 남았다");
        assertThat(updated.getRating()).isEqualByComparingTo("4.5");
        assertThat(updated.getExpenseAmount()).isEqualByComparingTo("15000");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());

        mockMvc.perform(get("/api/entries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().json(body));
    }

    @Test
    @DisplayName("수정 시 감상·평점 제거")
    void clearOptionalFields() throws Exception {
        CultureEntry entry = saveEntry("기존 제목", LocalDate.of(2026, 1, 1));
        entry.update(entry.getTitle(), entry.getCategory(), entry.getExperiencedOn(),
                "감상", new BigDecimal("4.5"), BigDecimal.ZERO);
        entryRepository.flush();

        mockMvc.perform(put("/api/entries/{id}", entry.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"수정 제목", "category":"BOOK",
                         "experiencedOn":"2026-01-01", "expenseAmount":0}
                        """))
                .andExpect(status().isOk());

        Long id = entry.getId();
        entityManager.clear();
        CultureEntry updated = entryRepository.findById(id).orElseThrow();
        assertThat(updated.getReview()).isNull();
        assertThat(updated.getRating()).isNull();
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    @DisplayName("수정 입력 검증 실패 시 기존 기록 유지")
    void rejectInvalidUpdate(String request) throws Exception {
        CultureEntry entry = saveEntry("기존 제목", LocalDate.of(2026, 1, 1));
        Long id = entry.getId();

        mockMvc.perform(put("/api/entries/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        entityManager.clear();
        assertThat(entryRepository.findById(id).orElseThrow().getTitle()).isEqualTo("기존 제목");
    }

    @Test
    @DisplayName("기록 삭제 후 조회 시 404 응답")
    void deleteEntry() throws Exception {
        Long id = saveEntry("삭제 대상", LocalDate.of(2026, 1, 1)).getId();

        mockMvc.perform(delete("/api/entries/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        entityManager.flush();
        entityManager.clear();
        assertThat(entryRepository.existsById(id)).isFalse();
        mockMvc.perform(get("/api/entries/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/entries/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("없는 기록 수정·삭제 시 404 응답")
    void rejectMissingEntry() throws Exception {
        mockMvc.perform(put("/api/entries/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTRY_NOT_FOUND"));

        mockMvc.perform(delete("/api/entries/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTRY_NOT_FOUND"));
    }

    private CultureEntry saveEntry(String title, LocalDate date) {
        return entryRepository.saveAndFlush(new CultureEntry(title, Category.BOOK,
                date, null, null, BigDecimal.ZERO));
    }
}
