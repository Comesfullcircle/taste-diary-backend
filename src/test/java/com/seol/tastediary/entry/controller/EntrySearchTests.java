package com.seol.tastediary.entry.controller;

import com.seol.tastediary.entry.domain.Category;
import com.seol.tastediary.entry.domain.CultureEntry;
import com.seol.tastediary.entry.repository.EntryRepository;
import com.seol.tastediary.member.domain.Member;
import com.seol.tastediary.member.repository.MemberRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "search-test@example.com")
class EntrySearchTests {
    @Autowired
    private MemberRepository memberRepository;

    private Long memberId;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntryRepository entryRepository;

    @BeforeEach
    void setUp() {
        memberId = memberRepository.saveAndFlush(new Member("search-test@example.com", "test-hash")).getId();
        entryRepository.deleteAll();
        saveEntry("Dune 영화", Category.MOVIE, 1);
        saveEntry("Dune 원작", Category.BOOK, 4);
        saveEntry("다른 영화", Category.MOVIE, 2);
        saveEntry("DUNE 재관람", Category.MOVIE, 3);
    }

    @Test
    @DisplayName("제목 부분 검색 및 영문 대소문자 무시")
    void searchByTitle() throws Exception {
        mockMvc.perform(get("/api/entries").param("keyword", "un"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Dune 원작"))
                .andExpect(jsonPath("$.content[1].title").value("DUNE 재관람"))
                .andExpect(jsonPath("$.content[2].title").value("Dune 영화"));
    }

    @Test
    @DisplayName("카테고리 단독 조회")
    void filterByCategory() throws Exception {
        mockMvc.perform(get("/api/entries").param("category", "BOOK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Dune 원작"));
    }

    @Test
    @DisplayName("검색·카테고리 동시 적용 및 페이지 이동")
    void searchWithCategory() throws Exception {
        mockMvc.perform(get("/api/entries").param("keyword", " dune ")
                        .param("category", "MOVIE").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("DUNE 재관람"));

        mockMvc.perform(get("/api/entries").param("keyword", "dune")
                        .param("category", "MOVIE").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Dune 영화"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("빈 검색어는 전체 목록 조회")
    void ignoreBlankKeyword(String keyword) throws Exception {
        mockMvc.perform(get("/api/entries").param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @DisplayName("빈 검색어에서도 카테고리 조건 유지")
    void filterWithBlankKeyword() throws Exception {
        mockMvc.perform(get("/api/entries").param("keyword", " ").param("category", "BOOK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 목록 응답")
    void returnEmptyResults() throws Exception {
        mockMvc.perform(get("/api/entries").param("keyword", "없는 제목"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/entries").param("keyword", "Dune").param("category", "EXHIBITION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "!", "\\"})
    @DisplayName("특수 문자는 입력한 그대로 검색")
    void searchLiteralCharacters(String keyword) throws Exception {
        saveEntry("특수 문자 " + keyword, Category.OTHER, 5);

        mockMvc.perform(get("/api/entries").param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("특수 문자 " + keyword));
    }

    @Test
    @DisplayName("같은 날짜의 검색 결과는 ID 내림차순")
    void sortSearchResultsById() throws Exception {
        saveEntry("Dune 추가 관람", Category.MOVIE, 3);

        mockMvc.perform(get("/api/entries").param("keyword", "Dune").param("category", "MOVIE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Dune 추가 관람"))
                .andExpect(jsonPath("$.content[1].title").value("DUNE 재관람"));
    }

    @Test
    @DisplayName("잘못된 카테고리는 400 응답")
    void rejectInvalidCategory() throws Exception {
        mockMvc.perform(get("/api/entries").param("category", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private void saveEntry(String title, Category category, int day) {
        entryRepository.saveAndFlush(new CultureEntry(memberId, title, category,
                LocalDate.of(2026, 1, day), null, null, BigDecimal.ZERO));
    }
}
