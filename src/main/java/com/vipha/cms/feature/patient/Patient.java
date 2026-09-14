package com.vipha.cms.feature.patient;

import com.vipha.cms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "patient")
@Getter
@Setter
@SQLRestriction("active = true")
public class Patient extends BaseEntity {

    @Column(nullable = false, unique = true, updatable = false)
    private String mrn;

    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false)
    private LocalDate dob;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String phone;
    private String email;
    private String address;
    private String bloodGroup;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    private String emergencyContactName;
    private String emergencyContactPhone;

    @Column(nullable = false)
    private boolean active = true;
}
