package com.seol.tastediary.entry.service;

import com.seol.tastediary.entry.domain.CultureEntry;
import com.seol.tastediary.entry.dto.EntryCreateRequest;
import com.seol.tastediary.entry.dto.EntryResponse;
import com.seol.tastediary.entry.repository.EntryRepository;
import com.seol.tastediary.global.exception.EntryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntryService {
    private final EntryRepository entryRepository;

    @Transactional
    public EntryResponse create(EntryCreateRequest request) {
        CultureEntry entry = new CultureEntry(request.title(), request.category(),
                request.experiencedOn(), request.review(), request.rating(), request.expenseAmount());
        return EntryResponse.from(entryRepository.save(entry));
    }

    public EntryResponse findById(Long id) {
        return EntryResponse.from(entryRepository.findById(id)
                .orElseThrow(() -> new EntryNotFoundException(id)));
    }
}
