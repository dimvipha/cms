package com.vipha.cms.feature.staff;

import com.vipha.cms.feature.staff.dto.CreateStaffRequest;
import com.vipha.cms.feature.staff.dto.StaffResponse;

public interface StaffService {
    StaffResponse create(CreateStaffRequest request);

    void triggerPasswordReset(String userId);
}
