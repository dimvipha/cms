package com.vipha.cms.feature.appointment;

import com.vipha.cms.feature.appointment.dto.AppointmentResponse;
import com.vipha.cms.feature.appointment.dto.CreateAppointmentRequest;
import com.vipha.cms.feature.appointment.dto.UpdateAppointmentRequest;
import com.vipha.cms.feature.patient.Patient;
import com.vipha.cms.security.UserNames;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class AppointmentMapper {

    @Autowired
    protected UserNames userNames;

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", expression = "java(patientName(appointment.getPatient()))")
    @Mapping(target = "doctorId", source = "doctor.userId")
    @Mapping(target = "doctorName", expression = "java(userNames.fullName(appointment.getDoctor().getUserId()))")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    public abstract AppointmentResponse toResponse(Appointment appointment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "status", ignore = true)
    public abstract Appointment toEntity(CreateAppointmentRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "status", ignore = true)
    public abstract void updateEntity(UpdateAppointmentRequest request, @MappingTarget Appointment appointment);

    protected String patientName(Patient patient) {
        return patient.getFirstName() + " " + patient.getLastName();
    }
}
