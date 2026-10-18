package com.seol.tastediary.entry.controller;

import com.seol.tastediary.entry.dto.EntryCreateRequest;
import com.seol.tastediary.entry.dto.EntryResponse;
import com.seol.tastediary.entry.service.EntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
