package com.nursing.home.repository;

import com.nursing.home.entity.MedicalEscort;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalEscortRepository extends JpaRepository<MedicalEscort, Long> {

    boolean existsByEscortNo(String escortNo);

    List<MedicalEscort> findAllByOrderByCreatedAtDesc();

    List<MedicalEscort> findByResidentIdOrderByCreatedAtAsc(Long residentId);

    Optional<MedicalEscort> findFirstByResidentIdAndStatusInOrderByCreatedAtDesc(
            Long residentId, Collection<String> statuses);

    List<MedicalEscort> findByStatusInOrderByExpectedReturnAtAsc(Collection<String> statuses);
}
