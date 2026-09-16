package com.nursing.home.repository;

import com.nursing.home.entity.Resident;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    boolean existsByCode(String code);

    List<Resident> findAllByOrderByUpdatedAtDesc();

    long countByRoomIdAndStatusIn(Long roomId, Collection<String> statuses);

    List<Resident> findByBedIdAndStatusIn(Long bedId, Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Resident r where r.id = :id")
    Optional<Resident> findByIdForUpdate(@Param("id") Long id);
}
