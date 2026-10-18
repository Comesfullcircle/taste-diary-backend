package com.seol.tastediary.entry.controller;

import com.seol.tastediary.entry.repository.EntryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
