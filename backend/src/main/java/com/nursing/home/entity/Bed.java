package com.nursing.home.entity;

import jakarta.persistence.*;

/** 床位：编号唯一，归属某个房间，住进老人后变成占用。 */
@Entity
@Table(name = "bed")
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(name = "room_id")
    public Long roomId;

    /** 靠窗 / 靠门 / 中间 */
    @Column(nullable = false, length = 16)
    public String position;

    /** 空闲 / 占用 / 停用 */
    @Column(nullable = false, length = 16)
    public String status;
}
