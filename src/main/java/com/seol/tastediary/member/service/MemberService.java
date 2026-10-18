package com.seol.tastediary.member.service;

import com.seol.tastediary.auth.dto.SignupRequest;
import com.seol.tastediary.global.exception.DuplicateEmailException;
import com.seol.tastediary.member.domain.Member;
import com.seol.tastediary.member.dto.MemberResponse;
import com.seol.tastediary.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }
        Member member = new Member(request.email(), passwordEncoder.encode(request.password()));
        try {
            return MemberResponse.from(memberRepository.saveAndFlush(member));
        } catch (DataIntegrityViolationException exception) {
            // 동시 가입 요청에서도 DB의 이메일 중복 제약으로 한 계정만 생성한다.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "uk_members_email".equals(violation.getConstraintName())) {
                    throw new DuplicateEmailException();
                }
            }
            throw exception;
        }
    }

    public MemberResponse findByEmail(String email) {
        return MemberResponse.from(memberRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("인증 정보를 확인해 주세요.")));
    }
}
