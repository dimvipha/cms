package com.vipha.cms.feature.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository
        extends JpaRepository<Appointment, UUID> {

    @Query("select a.appointmentTime from Appointment a " +
            "where a.doctor.userId = :doctorUserId " +
            "and a.appointmentDate = :date " +
            "and a.status <> :excludedStatus")
    List<LocalTime> findBookedTimes(
            @Param("doctorUserId") String doctorUserId,
            @Param("date") LocalDate date,
            @Param("excludedStatus") AppointmentStatus excludedStatus);

    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(
            UUID patientId
    );

    boolean existsByDoctorUserIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            String doctorUserId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            AppointmentStatus status
    );

    boolean existsByDoctorUserIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
            String doctorUserId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            AppointmentStatus status,
            UUID appointmentId
    );

    boolean existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            UUID patientId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            AppointmentStatus status
    );
}
