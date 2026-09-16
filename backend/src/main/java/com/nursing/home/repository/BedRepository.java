package com.nursing.home.repository;

import com.nursing.home.entity.Bed;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BedRepository extends JpaRepository<Bed, Long> {

    boolean existsByCode(String code);

    long countByRoomIdAndStatusIn(Long roomId, Collection<String> statuses);

    List<Bed> findByRoomIdAndStatusIn(Long roomId, Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bed b where b.id = :id")
    Optional<Bed> findByIdForUpdate(@Param("id") Long id);
}
