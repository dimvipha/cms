package com.vipha.cms.feature.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AvailabilityService {
    List<LocalTime> getAvailableSlots(String doctorUserId, LocalDate date);
}
