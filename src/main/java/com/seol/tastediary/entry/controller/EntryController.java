package com.seol.tastediary.entry.controller;

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
    public ResponseEntity<EntryResponse> create(@Valid @RequestBody EntryCreateRequest request) {
        EntryResponse response = entryService.create(request);
        return ResponseEntity.created(URI.create("/api/entries/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public EntryResponse findById(@PathVariable Long id) {
        return entryService.findById(id);
    }

    @GetMapping
    public EntryListResponse findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return entryService.findAll(page, size);
    }

    @PutMapping("/{id}")
    public EntryResponse update(@PathVariable Long id, @Valid @RequestBody EntryUpdateRequest request) {
        return entryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
