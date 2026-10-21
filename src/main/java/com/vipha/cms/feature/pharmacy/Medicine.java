package com.vipha.cms.feature.pharmacy;

import com.vipha.cms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A discontinued medicine ({@code active = false}) stays loadable so old
 * prescriptions and invoices keep working; the catalogue and new prescriptions leave it out.
 */
@Entity
@Table(name = "medicine")
@Getter
@Setter
@BatchSize(size = 50) // prescription lists load item medicines in one round trip
public class Medicine extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(length = 30)
    private String form;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer reorderLevel = 0;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "medicine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicineBatch> batches = new ArrayList<>();
}
