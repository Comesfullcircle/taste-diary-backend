package com.seol.tastediary.entry.repository;

import com.seol.tastediary.entry.domain.CultureEntry;
import com.seol.tastediary.entry.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntryRepository extends JpaRepository<CultureEntry, Long> {

    @Query("""
            SELECT e FROM CultureEntry e
            WHERE LOWER(e.title) LIKE LOWER(:keyword) ESCAPE '!'
              AND (:category IS NULL OR e.category = :category)
            """)
    Page<CultureEntry> search(@Param("keyword") String keyword,
                              @Param("category") Category category, Pageable pageable);
}
