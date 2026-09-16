package com.nursing.home.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 外出就医护送单：单未销前档案为请假外出，床位仍保持占用。 */
@Entity
@Table(name = "medical_escort")
public class MedicalEscort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "escort_no", nullable = false, unique = true, length = 32)
    public String escortNo;

    @Column(name = "resident_id", nullable = false)
    public Long residentId;

    @Column(name = "hospital_name", nullable = false, length = 128)
    @JsonAlias({"hospital", "hospitalTitle"})
    public String hospitalName;

    @Column(name = "expected_leave_at", nullable = false)
    @JsonAlias({"expectedDepartureAt", "leaveAt", "departureAt", "plannedLeaveAt"})
    public LocalDateTime expectedLeaveAt;

    @Column(name = "expected_return_at", nullable = false)
    @JsonAlias({"plannedReturnAt", "returnAt"})
    public LocalDateTime expectedReturnAt;

    @Column(name = "escort_name", nullable = false, length = 32)
    @JsonAlias({"escort", "escortPerson", "escortPersonName"})
    public String escortName;

    /** 护理员 / 家属 */
    @Column(name = "escort_type", nullable = false, length = 16)
    public String escortType;

    /** 护送中 / 滞留 / 已销单 */
    @Column(nullable = false, length = 16)
    public String status;

    @Column(name = "closed_at")
    public LocalDateTime closedAt;

    /** 人已回院 / 接手护送 */
    @Column(name = "close_action", length = 32)
    public String closeAction;

    @Column(length = 32)
    public String confirmer;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
