package com.nursing.home.repository;

import com.nursing.home.entity.Bed;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BedRepository extends JpaRepository<Bed, Long> {

    boolean existsByCode(String code);

    long countByRoomIdAndStatusIn(Long roomId, Collection<String> statuses);

    List<Bed> findByRoomIdAndStatusIn(Long roomId, Collection<String> statuses);
}
