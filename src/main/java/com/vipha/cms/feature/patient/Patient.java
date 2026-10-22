package com.vipha.cms.feature.patient;

import com.vipha.cms.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A closed record ({@code active = false}) stays loadable so its appointments,
 * visits and invoices keep working; lists and new bookings leave it out.
 */
@Entity
@Table(name = "patient")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Patient extends BaseEntity {

    @Column(nullable = false, unique = true, updatable = false)
    private String mrn;

    @Column(unique = true, updatable = false)
    private String keycloakId;

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
