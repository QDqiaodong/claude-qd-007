package com.nursing.home.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 老人档案：档案号唯一，一位老人同时只在一张床上。 */
@Entity
@Table(name = "resident")
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 32)
    public String name;

    /** 男 / 女 */
    @Column(nullable = false, length = 8)
    public String gender;

    @Column(nullable = false)
    public Integer age;

    /** 自理 / 半自理 / 不能自理 */
    @Column(name = "care_level", nullable = false, length = 16)
    public String careLevel;

    @Column(name = "room_id")
    public Long roomId;

    @Column(name = "bed_id")
    public Long bedId;

    @Column(name = "check_in_date")
    public LocalDate checkInDate;

    @Column(name = "check_out_date")
    public LocalDate checkOutDate;

    @Column(name = "family_phone", length = 20)
    public String familyPhone;

    /** 在住 / 已退住 / 请假外出 */
    @Column(nullable = false, length = 16)
    public String status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
