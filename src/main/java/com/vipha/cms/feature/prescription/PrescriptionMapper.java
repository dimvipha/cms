package com.vipha.cms.feature.prescription;

import com.vipha.cms.feature.prescription.dto.PrescriptionItemRequest;
import com.vipha.cms.feature.prescription.dto.PrescriptionItemResponse;
import com.vipha.cms.feature.prescription.dto.PrescriptionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PrescriptionMapper {

    @Mapping(target = "visitId", source = "visit.id")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "doctorId", source = "doctor.userId")
    PrescriptionResponse toResponse(Prescription prescription);

    @Mapping(target = "medicineId", source = "medicine.id")
    @Mapping(target = "medicineCode", source = "medicine.code")
    @Mapping(target = "medicineName", source = "medicine.name")
    @Mapping(target = "unit", source = "medicine.unit")
    PrescriptionItemResponse toItemResponse(PrescriptionItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "prescription", ignore = true)
    @Mapping(target = "medicine", ignore = true)
    PrescriptionItem toItem(PrescriptionItemRequest request);
}
