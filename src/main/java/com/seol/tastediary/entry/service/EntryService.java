package com.seol.tastediary.entry.service;

import com.seol.tastediary.entry.domain.CultureEntry;
import com.seol.tastediary.entry.dto.EntryCreateRequest;
import com.seol.tastediary.entry.dto.EntryResponse;
import com.seol.tastediary.entry.dto.EntryListResponse;
import com.seol.tastediary.entry.dto.EntryUpdateRequest;
import com.seol.tastediary.entry.repository.EntryRepository;
import com.seol.tastediary.global.exception.EntryNotFoundException;
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

    @Transactional
    public EntryResponse create(EntryCreateRequest request) {
        CultureEntry entry = new CultureEntry(request.title(), request.category(),
                request.experiencedOn(), request.review(), request.rating(), request.expenseAmount());
        return EntryResponse.from(entryRepository.save(entry));
    }

    public EntryResponse findById(Long id) {
        return EntryResponse.from(getEntry(id));
    }

    public EntryListResponse findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "experiencedOn", "id"));
        return EntryListResponse.from(entryRepository.findAll(pageable).map(EntryResponse::from));
    }

    @Transactional
    public EntryResponse update(Long id, EntryUpdateRequest request) {
        CultureEntry entry = getEntry(id);
        entry.update(request.title(), request.category(), request.experiencedOn(),
                request.review(), request.rating(), request.expenseAmount());
        entryRepository.flush();
        return EntryResponse.from(entry);
    }

    @Transactional
    public void delete(Long id) {
        entryRepository.delete(getEntry(id));
    }

    private CultureEntry getEntry(Long id) {
        return entryRepository.findById(id).orElseThrow(() -> new EntryNotFoundException(id));
    }
}
