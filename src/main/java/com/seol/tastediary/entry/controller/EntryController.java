package com.seol.tastediary.entry.controller;

import com.seol.tastediary.entry.domain.Category;
import com.seol.tastediary.entry.dto.EntryCreateRequest;
import com.seol.tastediary.entry.dto.EntryResponse;
import com.seol.tastediary.entry.dto.EntryListResponse;
import com.seol.tastediary.entry.dto.EntryUpdateRequest;
import com.seol.tastediary.entry.service.EntryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class EntryController {
    private final EntryService entryService;

    @PostMapping
    public ResponseEntity<EntryResponse> create(@Valid @RequestBody EntryCreateRequest request,
                                                Authentication authentication) {
        EntryResponse response = entryService.create(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/entries/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public EntryResponse findById(@PathVariable Long id, Authentication authentication) {
        return entryService.findById(authentication.getName(), id);
    }

    @GetMapping
    public EntryListResponse findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Category category, Authentication authentication) {
        return entryService.findAll(authentication.getName(), page, size, keyword, category);
    }

    @PutMapping("/{id}")
    public EntryResponse update(@PathVariable Long id, @Valid @RequestBody EntryUpdateRequest request,
                                Authentication authentication) {
        return entryService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        entryService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
