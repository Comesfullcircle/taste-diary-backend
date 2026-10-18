package com.seol.tastediary.entry.service;

import com.seol.tastediary.entry.domain.CultureEntry;
import com.seol.tastediary.entry.domain.Category;
import com.seol.tastediary.entry.dto.EntryCreateRequest;
import com.seol.tastediary.entry.dto.EntryResponse;
import com.seol.tastediary.entry.dto.EntryListResponse;
import com.seol.tastediary.entry.dto.EntryUpdateRequest;
import com.seol.tastediary.entry.repository.EntryRepository;
import com.seol.tastediary.global.exception.EntryNotFoundException;
import com.seol.tastediary.member.repository.MemberRepository;
import org.springframework.security.authentication.BadCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntryService {
    private final EntryRepository entryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public EntryResponse create(String email, EntryCreateRequest request) {
        CultureEntry entry = new CultureEntry(getMemberId(email), request.title(), request.category(),
                request.experiencedOn(), request.review(), request.rating(), request.expenseAmount());
        return EntryResponse.from(entryRepository.save(entry));
    }

    public EntryResponse findById(String email, Long id) {
        return EntryResponse.from(getEntry(id, getMemberId(email)));
    }

    public EntryListResponse findAll(String email, int page, int size, String keyword, Category category) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "experiencedOn", "id"));
        String pattern = "%";
        if (keyword != null && !keyword.isBlank()) {
            String escaped = keyword.strip().replace("!", "!!")
                    .replace("%", "!%").replace("_", "!_");
            pattern = "%" + escaped + "%";
        }
        return EntryListResponse.from(entryRepository.search(getMemberId(email), pattern, category, pageable)
                .map(EntryResponse::from));
    }

    @Transactional
    public EntryResponse update(String email, Long id, EntryUpdateRequest request) {
        CultureEntry entry = getEntry(id, getMemberId(email));
        entry.update(request.title(), request.category(), request.experiencedOn(),
                request.review(), request.rating(), request.expenseAmount());
        entryRepository.flush();
        return EntryResponse.from(entry);
    }

    @Transactional
    public void delete(String email, Long id) {
        entryRepository.delete(getEntry(id, getMemberId(email)));
    }

    private CultureEntry getEntry(Long id, Long memberId) {
        return entryRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new EntryNotFoundException(id));
    }

    private Long getMemberId(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("인증 정보를 확인해 주세요."))
                .getId();
    }
}
