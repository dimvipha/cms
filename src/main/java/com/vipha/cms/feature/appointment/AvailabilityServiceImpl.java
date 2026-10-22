package com.vipha.cms.feature.appointment;

import com.vipha.cms.common.exception.BusinessRuleException;
import com.vipha.cms.common.exception.ResourceNotFoundException;
import com.vipha.cms.feature.doctor.Doctor;
import com.vipha.cms.feature.doctor.DoctorRepository;
import com.vipha.cms.feature.doctor.DoctorSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final Duration SLOT = Duration.ofMinutes(30);

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public List<LocalTime> getAvailableSlots(String doctorUserId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Date cannot be in the past");
        }

        Doctor doctor = doctorRepository.findById(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (!doctor.isActive()) {
            return List.of();
        }

        List<DoctorSchedule> windows = doctor.getSchedules().stream()
                .filter(s -> s.getDayOfWeek() == date.getDayOfWeek())
                .toList();
        if (windows.isEmpty()) {
            return List.of();
        }

        Set<LocalTime> booked = new HashSet<>(
                appointmentRepository.findBookedTimes(doctorUserId, date, AppointmentStatus.CANCELLED));

        LocalTime cutoff = date.equals(LocalDate.now()) ? LocalTime.now() : LocalTime.MIN;

        List<LocalTime> slots = new ArrayList<>();
        for (DoctorSchedule window : windows) {
            LocalTime slot = window.getStartTime();
            while (!slot.plus(SLOT).isAfter(window.getEndTime())) {
                if (!booked.contains(slot) && slot.isAfter(cutoff)) {
                    slots.add(slot);
                }
                slot = slot.plus(SLOT);
            }
        }
        return slots;
    }
}
