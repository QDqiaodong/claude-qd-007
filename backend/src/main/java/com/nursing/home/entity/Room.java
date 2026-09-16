package com.nursing.home.entity;

import jakarta.persistence.*;

/** 房间：编号唯一，停用前要把在住老人和当天班次都安置好。 */
@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    @Column(nullable = false)
    public Integer floor;

    /** 单人间 / 双人间 / 多人间 */
    @Column(nullable = false, length = 16)
    public String kind;

    /** 可住人数 */
    @Column(nullable = false)
    public Integer capacity;

    /** 在用 / 停用 / 维修 */
    @Column(nullable = false, length = 16)
    public String status;
}
