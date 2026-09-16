package com.nursing.home.repository;

import com.nursing.home.entity.CareShift;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareShiftRepository extends JpaRepository<CareShift, Long> {

    boolean existsByShiftNo(String shiftNo);

    List<CareShift> findAllByOrderByUpdatedAtDesc();

    List<CareShift> findByShiftDateOrderByStartMinAsc(LocalDate shiftDate);

    List<CareShift> findByRoomIdAndShiftDateAndStatusNotIn(
            Long roomId, LocalDate shiftDate, Collection<String> statuses);

    List<CareShift> findByNurseAndShiftDateAndStatusNotIn(
            String nurse, LocalDate shiftDate, Collection<String> statuses);
}
