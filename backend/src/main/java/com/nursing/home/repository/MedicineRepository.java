package com.nursing.home.repository;

import com.nursing.home.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    boolean existsByCode(String code);
}
