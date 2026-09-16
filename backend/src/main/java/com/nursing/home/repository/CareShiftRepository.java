package com.nursing.home.repository;

import com.nursing.home.entity.CareShift;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CareShiftRepository extends JpaRepository<CareShift, Long> {

    boolean existsByShiftNo(String shiftNo);

    List<CareShift> findAllByOrderByUpdatedAtDesc();

    List<CareShift> findByShiftDateOrderByStartMinAsc(LocalDate shiftDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CareShift s where s.id = :id")
    Optional<CareShift> findByIdForUpdate(@Param("id") Long id);

    List<CareShift> findByRoomIdAndShiftDateAndStatusIn(
            Long roomId, LocalDate shiftDate, Collection<String> statuses);

    List<CareShift> findByRoomIdAndStatusIn(Long roomId, Collection<String> statuses);

    List<CareShift> findByNurseAndShiftDateAndStatusIn(
            String nurse, LocalDate shiftDate, Collection<String> statuses);
}
