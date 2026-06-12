package com.paradissaveurs.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_zones")
public class DeliveryZoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer fee;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getFee() { return fee; }
    public void setFee(Integer fee) { this.fee = fee; }
}
