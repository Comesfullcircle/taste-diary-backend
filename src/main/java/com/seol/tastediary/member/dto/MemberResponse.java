package com.seol.tastediary.member.dto;

import com.seol.tastediary.member.domain.Member;

import java.time.Instant;

public record MemberResponse(Long id, String email, Instant createdAt) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getCreatedAt());
    }
}
