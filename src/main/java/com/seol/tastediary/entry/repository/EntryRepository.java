package com.seol.tastediary.entry.repository;

import com.seol.tastediary.entry.domain.CultureEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<CultureEntry, Long> {
}
