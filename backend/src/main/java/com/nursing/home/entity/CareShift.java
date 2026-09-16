package com.nursing.home.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 护理班次：哪间房在某天的某一段由哪位护理员值守。 */
@Entity
@Table(name = "care_shift")
public class CareShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "shift_no", nullable = false, unique = true, length = 32)
    public String shiftNo;

    @Column(name = "room_id", nullable = false)
    public Long roomId;

    @Column(name = "shift_date", nullable = false)
    public LocalDate shiftDate;

    /** 早班 / 中班 / 夜班 */
    @Column(nullable = false, length = 16)
    public String period;

    @Column(nullable = false, length = 32)
    public String nurse;

    /** 从 0:00 起算的分钟数 */
    @Column(name = "start_min", nullable = false)
    public Integer startMin;

    @Column(name = "end_min", nullable = false)
    public Integer endMin;

    /** 交班时写的注意事项 */
    @Column(name = "handover_note", length = 255)
    public String handoverNote;

    /** 待接班 / 值班中 / 已交班 / 已取消 */
    @Column(nullable = false, length = 16)
    public String status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
