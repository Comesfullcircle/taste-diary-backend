package com.seol.tastediary.auth.controller;

import com.seol.tastediary.member.domain.Member;
import com.seol.tastediary.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {
    private static final String PASSWORD = "test-password-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 및 비밀번호 해시 저장")
    void signup() throws Exception {
        String email = newEmail();
        MvcResult result = mockMvc.perform(post("/api/auth/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        Member member = memberRepository.findByEmail(email).orElseThrow();
        assertThat(member.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, member.getPasswordHash())).isTrue();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(member.getPasswordHash());
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("이메일 정규화 및 중복 가입 거부")
    void rejectDuplicateEmail() throws Exception {
        String email = newEmail();
        mockMvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @ParameterizedTest
    @MethodSource("invalidSignups")
    @DisplayName("가입 입력 검증 실패 시 저장 방지")
    void rejectInvalidSignup(String body) throws Exception {
        long count = memberRepository.count();
        mockMvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        assertThat(memberRepository.count()).isEqualTo(count);
    }

    static Stream<String> invalidSignups() {
        return Stream.of(
                body("", PASSWORD), body("invalid", PASSWORD), body("user@example.com", "short"),
                body("user@example.com", "        "), body("user@example.com", "a".repeat(73)),
                body("user@example.com", "가".repeat(25)),
                "{\"email\":null,\"password\":null}", "{}");
    }

    @Test
    @DisplayName("실제 CSRF 토큰으로 가입·로그인·내 정보·로그아웃")
    void sessionLoginFlow() throws Exception {
        String email = newEmail();
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        String token = tokenFrom(csrfResult);
        String originalSessionId = session.getId();

        mockMvc.perform(post("/api/auth/signup").session(session).header("X-CSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(email, PASSWORD)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").session(session)
                        .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                        .content(body(email.toUpperCase(java.util.Locale.ROOT), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session.getId()).isNotEqualTo(originalSessionId);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN", token))
                .andExpect(status().isForbidden());

        MvcResult newCsrf = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        mockMvc.perform(post("/api/auth/logout").session(session)
                        .header("X-CSRF-TOKEN", tokenFrom(newCsrf)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("없는 이메일과 잘못된 비밀번호는 동일한 인증 오류")
    void rejectInvalidCredentials() throws Exception {
        String email = newEmail();
        mockMvc.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, PASSWORD)))
                .andExpect(status().isCreated());

        MvcResult wrongPassword = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(email, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();
        mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(newEmail(), PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(wrongPassword.getResponse().getContentAsString()));
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CSRF 토큰 없는 변경 요청 거부")
    void rejectMissingCsrf() throws Exception {
        for (String endpoint : new String[]{"signup", "login", "logout"}) {
            mockMvc.perform(post("/api/auth/" + endpoint).contentType(MediaType.APPLICATION_JSON)
                            .content(body(newEmail(), PASSWORD)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
        }
    }

    @Test
    @DisplayName("잘못된 CSRF 토큰 거부")
    void rejectInvalidCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body(newEmail(), PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
    }

    @Test
    @DisplayName("긴 비밀번호 로그인 요청은 400 응답")
    void rejectLongLoginPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(newEmail(), "가".repeat(25))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private String tokenFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asString();
    }

    private static String newEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    private static String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
