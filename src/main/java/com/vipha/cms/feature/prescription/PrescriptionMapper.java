package com.vipha.cms.feature.prescription;

import com.vipha.cms.feature.prescription.dto.PrescriptionItemRequest;
import com.vipha.cms.feature.prescription.dto.PrescriptionItemResponse;
import com.vipha.cms.feature.prescription.dto.PrescriptionResponse;
import com.vipha.cms.security.UserNames;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class PrescriptionMapper {

    @Autowired
    protected UserNames userNames;

    @Mapping(target = "visitId", source = "visit.id")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName",
            expression = "java(prescription.getPatient().getFirstName() + \" \" + prescription.getPatient().getLastName())")
    @Mapping(target = "doctorId", source = "doctor.userId")
    @Mapping(target = "doctorName", expression = "java(userNames.fullName(prescription.getDoctor().getUserId()))")
    public abstract PrescriptionResponse toResponse(Prescription prescription);

    @Mapping(target = "medicineId", source = "medicine.id")
    @Mapping(target = "medicineCode", source = "medicine.code")
    @Mapping(target = "medicineName", source = "medicine.name")
    @Mapping(target = "unit", source = "medicine.unit")
    public abstract PrescriptionItemResponse toItemResponse(PrescriptionItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "prescription", ignore = true)
    @Mapping(target = "medicine", ignore = true)
    public abstract PrescriptionItem toItem(PrescriptionItemRequest request);
}
