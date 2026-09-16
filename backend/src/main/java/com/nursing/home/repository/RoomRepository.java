package com.nursing.home.repository;

import com.nursing.home.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByCode(String code);
}
