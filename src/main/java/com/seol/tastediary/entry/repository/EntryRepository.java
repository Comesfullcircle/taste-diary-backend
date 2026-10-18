package com.seol.tastediary.entry.repository;

import com.seol.tastediary.entry.domain.CultureEntry;
import com.seol.tastediary.entry.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface EntryRepository extends JpaRepository<CultureEntry, Long> {

    Optional<CultureEntry> findByIdAndMemberId(Long id, Long memberId);

    @Query("""
            SELECT e FROM CultureEntry e
            WHERE e.memberId = :memberId
              AND LOWER(e.title) LIKE LOWER(:keyword) ESCAPE '!'
              AND (:category IS NULL OR e.category = :category)
            """)
    Page<CultureEntry> search(@Param("memberId") Long memberId, @Param("keyword") String keyword,
                              @Param("category") Category category, Pageable pageable);
}
