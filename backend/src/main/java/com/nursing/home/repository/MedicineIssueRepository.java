package com.nursing.home.repository;

import com.nursing.home.entity.MedicineIssue;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineIssueRepository extends JpaRepository<MedicineIssue, Long> {

    List<MedicineIssue> findAllByOrderByCreatedAtDesc();

    List<MedicineIssue> findByResidentIdOrderByCreatedAtAsc(Long residentId);

    List<MedicineIssue> findByResidentIdAndMedicineIdAndIssueDateAndDoseTimeAndKind(
            Long residentId, Long medicineId, LocalDate issueDate, String doseTime, String kind);
}
