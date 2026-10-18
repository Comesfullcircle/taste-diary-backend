package com.seol.tastediary.entry.controller;

import com.seol.tastediary.entry.domain.Category;
import com.seol.tastediary.entry.domain.CultureEntry;
import com.seol.tastediary.entry.repository.EntryRepository;
import com.seol.tastediary.member.domain.Member;
import com.seol.tastediary.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EntryOwnershipTests {
    private static final String REQUEST = """
            {"title":"새 기록","category":"MOVIE","experiencedOn":"2026-01-01","expenseAmount":0}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntryRepository entryRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    private Member owner;
    private Member other;
    private Long entryId;

    @BeforeEach
    void setUp() {
        owner = memberRepository.saveAndFlush(new Member(
                UUID.randomUUID() + "@example.com", "test-hash"));
        other = memberRepository.saveAndFlush(new Member(
                UUID.randomUUID() + "@example.com", "test-hash"));
        entryId = saveEntry(owner.getId(), "검색 기록 1", 1).getId();
        saveEntry(owner.getId(), "검색 기록 2", 2);
        saveEntry(owner.getId(), "다른 제목", 3);
        saveEntry(other.getId(), "검색 타인 기록 1", 4);
        saveEntry(other.getId(), "검색 타인 기록 2", 5);
    }

    @Test
    @DisplayName("로그인 회원을 작성자로 저장하고 임의 작성자 값 무시")
    void createWithCurrentMember() throws Exception {
        String request = REQUEST.replace("\"title\"", "\"memberId\":" + other.getId() + ",\"title\"");
        MvcResult result = mockMvc.perform(post("/api/entries").with(user(owner.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andReturn();

        Long id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        entityManager.flush();
        entityManager.clear();
        assertThat(entryRepository.findById(id).orElseThrow().getMemberId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("목록·검색·페이지 집계에서 다른 회원 기록 제외")
    void listOnlyOwnEntries() throws Exception {
        mockMvc.perform(get("/api/entries").with(user(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].title").value("다른 제목"));

        mockMvc.perform(get("/api/entries").with(user(owner.getEmail()))
                        .param("keyword", "검색").param("category", "MOVIE")
                        .param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].title").value("검색 기록 2"));

        mockMvc.perform(get("/api/entries").with(user(owner.getEmail()))
                        .param("keyword", "검색").param("category", "MOVIE")
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("검색 기록 1"));

        mockMvc.perform(get("/api/entries").with(user(other.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("검색 타인 기록 2"));

        mockMvc.perform(get("/api/entries").with(user(owner.getEmail())).param("category", "MOVIE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("타인에게만 일치하는 검색 결과는 빈 목록")
    void excludeOtherMembersMatches() throws Exception {
        mockMvc.perform(get("/api/entries").with(user(owner.getEmail())).param("keyword", "타인"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    @DisplayName("타인 기록 접근은 없는 기록과 동일한 404 응답")
    void rejectOtherMembersEntry(String method) throws Exception {
        MvcResult missing = mockMvc.perform(entryRequest(method, Long.MAX_VALUE)
                        .with(user(other.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTRY_NOT_FOUND")).andReturn();

        mockMvc.perform(entryRequest(method, entryId).with(user(other.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTRY_NOT_FOUND"));

        assertThat(missing.getResponse().getContentAsString()).doesNotContain("memberId");
        entityManager.flush();
        entityManager.clear();
        CultureEntry entry = entryRepository.findById(entryId).orElseThrow();
        assertThat(entry.getTitle()).isEqualTo("검색 기록 1");
        assertThat(entry.getMemberId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("수정 요청으로 작성자 변경 불가")
    void keepOwnerOnUpdate() throws Exception {
        String request = REQUEST.replace("\"title\"", "\"memberId\":" + other.getId() + ",\"title\"");
        mockMvc.perform(put("/api/entries/{id}", entryId).with(user(owner.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk());
        entityManager.clear();
        assertThat(entryRepository.findById(entryId).orElseThrow().getMemberId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("익명 요청은 CSRF 토큰 유무와 무관하게 401 응답")
    void rejectAnonymousRequests() throws Exception {
        for (boolean withCsrf : new boolean[]{false, true}) {
            for (String method : new String[]{"GET", "PUT", "DELETE", "POST", "LIST"}) {
                MockHttpServletRequestBuilder request = switch (method) {
                    case "POST" -> post("/api/entries");
                    case "LIST" -> get("/api/entries");
                    default -> entryRequest(method, entryId);
                };
                if (withCsrf) {
                    request.with(csrf());
                }
                mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
            }
        }
    }

    @Test
    @DisplayName("로그인 회원도 CSRF 토큰 없는 수정은 거부")
    void requireCsrfForAuthenticatedMember() throws Exception {
        mockMvc.perform(put("/api/entries/{id}", entryId).with(user(owner.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
    }

    @Test
    @DisplayName("인증 정보에 해당하는 회원이 없으면 접근 거부")
    void rejectMissingMember() throws Exception {
        mockMvc.perform(get("/api/entries").with(user("missing-member@example.com")))
                .andExpect(status().isUnauthorized());
        long count = entryRepository.count();
        mockMvc.perform(post("/api/entries").with(user("missing-member@example.com")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnauthorized());
        assertThat(entryRepository.count()).isEqualTo(count);
    }

    @Test
    @DisplayName("실제 로그인 세션으로 작성 후 로그아웃하면 접근 불가")
    void createWithLoginSession() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        Member member = memberRepository.saveAndFlush(new Member(email,
                passwordEncoder.encode("test-password-123")));
        MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"test-password-123\"}"))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        MvcResult created = mockMvc.perform(post("/api/entries").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        assertThat(entryRepository.findById(id).orElseThrow().getMemberId()).isEqualTo(member.getId());
        mockMvc.perform(get("/api/entries/{id}", id).session(session))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/entries/{id}", id))
                .andExpect(status().isUnauthorized());
    }

    private CultureEntry saveEntry(Long memberId, String title, int day) {
        return entryRepository.saveAndFlush(new CultureEntry(memberId, title, Category.MOVIE,
                LocalDate.of(2026, 1, day), null, null, BigDecimal.ZERO));
    }

    private MockHttpServletRequestBuilder entryRequest(String method, long id) {
        return switch (method) {
            case "GET" -> get("/api/entries/{id}", id);
            case "PUT" -> put("/api/entries/{id}", id);
            case "DELETE" -> delete("/api/entries/{id}", id);
            default -> throw new IllegalArgumentException(method);
        };
    }
}
