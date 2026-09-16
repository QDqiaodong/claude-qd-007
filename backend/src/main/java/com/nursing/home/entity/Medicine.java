package com.nursing.home.entity;

import jakarta.persistence.*;

/** 药品：编号唯一，带库存与预警线。 */
@Entity
@Table(name = "medicine")
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    /** 片 / 支 / 袋 / 毫升 */
    @Column(nullable = false, length = 16)
    public String unit;

    /** 处方药 / 非处方 */
    @Column(nullable = false, length = 16)
    public String kind;

    @Column(nullable = false)
    public Integer stock;

    @Column(name = "warn_stock", nullable = false)
    public Integer warnStock;

    /** 在用 / 停用 */
    @Column(nullable = false, length = 16)
    public String status;
}
