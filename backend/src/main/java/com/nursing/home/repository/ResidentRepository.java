package com.nursing.home.repository;

import com.nursing.home.entity.Resident;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    boolean existsByCode(String code);

    List<Resident> findAllByOrderByUpdatedAtDesc();

    long countByRoomIdAndStatus(Long roomId, String status);

    List<Resident> findByBedIdAndStatusIn(Long bedId, Collection<String> statuses);
}
