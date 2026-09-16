package com.nursing.home.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 药品发放流水：给哪位老人发/退哪种药，早中晚哪一剂。 */
@Entity
@Table(name = "medicine_issue")
public class MedicineIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "resident_id", nullable = false)
    public Long residentId;

    @Column(name = "medicine_id", nullable = false)
    public Long medicineId;

    @Column(nullable = false)
    public Integer qty;

    /** 发放 / 退回 */
    @Column(nullable = false, length = 16)
    public String kind;

    /** 早 / 中 / 晚 */
    @Column(nullable = false, length = 8)
    public String doseTime;

    @Column(name = "issue_date", nullable = false)
    public LocalDate issueDate;

    @Column(length = 32)
    public String operator;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
